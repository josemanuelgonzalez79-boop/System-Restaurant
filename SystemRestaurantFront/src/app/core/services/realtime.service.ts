import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { filter, Observable, Subject } from 'rxjs';

import { isRealtimeEvent, RealtimeConnectionState, RealtimeEvent } from '../models/realtime.model';

interface BranchWatch {
  observers: number;
  subscription?: StompSubscription;
}

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private readonly state = signal<RealtimeConnectionState>('IDLE');
  private readonly events = new Subject<RealtimeEvent>();
  private readonly watchedBranches = new Map<number, BranchWatch>();
  private readonly client = this.createClient();

  readonly connectionState = this.state.asReadonly();

  watchBranch(branchId: number): Observable<RealtimeEvent> {
    return new Observable<RealtimeEvent>((observer) => {
      const branchWatch = this.watchedBranches.get(branchId) ?? { observers: 0 };
      branchWatch.observers += 1;
      this.watchedBranches.set(branchId, branchWatch);
      this.ensureConnected();
      this.subscribeBranch(branchId, branchWatch);

      const eventSubscription = this.events
        .pipe(filter((event) => event.branchId === branchId))
        .subscribe(observer);

      return () => {
        eventSubscription.unsubscribe();
        const current = this.watchedBranches.get(branchId);
        if (!current) {
          return;
        }
        current.observers -= 1;
        if (current.observers <= 0) {
          current.subscription?.unsubscribe();
          this.watchedBranches.delete(branchId);
          this.disconnectWhenUnused();
        }
      };
    });
  }

  disconnect(): void {
    this.watchedBranches.forEach((watch) => watch.subscription?.unsubscribe());
    this.watchedBranches.clear();
    void this.client.deactivate().finally(() => this.state.set('IDLE'));
  }

  private createClient(): Client {
    const client = new Client({
      brokerURL: this.websocketUrl(),
      reconnectDelay: 3_000,
      connectionTimeout: 8_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
    });
    client.onConnect = () => {
      this.state.set('CONNECTED');
      this.watchedBranches.forEach((watch, branchId) => {
        watch.subscription = undefined;
        this.subscribeBranch(branchId, watch);
      });
    };
    client.onWebSocketClose = () => {
      this.clearBrokerSubscriptions();
      this.state.set(client.active ? 'RECONNECTING' : 'IDLE');
    };
    client.onWebSocketError = () => this.state.set('RECONNECTING');
    client.onHeartbeatLost = () => this.state.set('RECONNECTING');
    client.onStompError = () => this.state.set('DISCONNECTED');
    return client;
  }

  private ensureConnected(): void {
    if (this.client.active) {
      return;
    }
    this.state.set('CONNECTING');
    this.client.activate();
  }

  private subscribeBranch(branchId: number, watch: BranchWatch): void {
    if (!this.client.connected || watch.subscription || watch.observers <= 0) {
      return;
    }
    watch.subscription = this.client.subscribe(`/topic/branches/${branchId}`, (message) =>
      this.handleMessage(message),
    );
  }

  private handleMessage(message: IMessage): void {
    try {
      const value: unknown = JSON.parse(message.body);
      if (isRealtimeEvent(value)) {
        this.events.next(value);
      }
    } catch {
      // Un mensaje inválido se ignora; REST conserva la fuente de verdad.
    }
  }

  private clearBrokerSubscriptions(): void {
    this.watchedBranches.forEach((watch) => (watch.subscription = undefined));
  }

  private disconnectWhenUnused(): void {
    queueMicrotask(() => {
      if (this.watchedBranches.size > 0 || !this.client.active) {
        return;
      }
      void this.client.deactivate().finally(() => {
        if (this.watchedBranches.size === 0) {
          this.state.set('IDLE');
        } else {
          this.ensureConnected();
        }
      });
    });
  }

  private websocketUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
  }
}
