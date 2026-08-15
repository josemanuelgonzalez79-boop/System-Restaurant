import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, interval, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TagModule } from 'primeng/tag';

import {
  PreparationItem,
  PreparationStatus,
  PreparationTicket,
} from '../../core/models/preparation.model';
import { Branch } from '../../core/models/structure.model';
import { OrderApiService } from '../../core/services/order-api.service';
import { PreparationApiService } from '../../core/services/preparation-api.service';

type DestinationFilter = 'ALL' | 'PRODUCTION' | 'SERVICE';

@Component({
  selector: 'app-preparation',
  imports: [ButtonModule, ConfirmDialogModule, FormsModule, TagModule],
  providers: [ConfirmationService],
  templateUrl: './preparation.html',
  styleUrl: './preparation.scss',
})
export class PreparationPage implements OnInit, OnDestroy {
  private readonly orderApi = inject(OrderApiService);
  private readonly preparationApi = inject(PreparationApiService);
  private readonly messages = inject(MessageService);
  private readonly confirmations = inject(ConfirmationService);
  private refreshSubscription?: Subscription;

  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly selectedBranchId = signal<number | null>(null);
  protected readonly destination = signal<DestinationFilter>('ALL');
  protected readonly showHistory = signal(false);
  protected readonly tickets = signal<PreparationTicket[]>([]);
  protected readonly changingIds = signal<Set<number>>(new Set());

  protected readonly columns: ReadonlyArray<{
    status: PreparationStatus;
    title: string;
    subtitle: string;
    icon: string;
  }> = [
    {
      status: 'PENDING',
      title: 'Por comenzar',
      subtitle: 'Comandas recién recibidas',
      icon: 'pi pi-clock',
    },
    {
      status: 'IN_PREPARATION',
      title: 'En preparación',
      subtitle: 'Trabajo en curso',
      icon: 'pi pi-bolt',
    },
    {
      status: 'READY',
      title: 'Listo para entregar',
      subtitle: 'Requiere salida a mesa o cliente',
      icon: 'pi pi-check-circle',
    },
  ];

  protected readonly activeItemCount = computed(() =>
    this.tickets().reduce(
      (total, ticket) =>
        total +
        ticket.items.filter((item) =>
          ['PENDING', 'IN_PREPARATION', 'READY'].includes(item.status),
        ).length,
      0,
    ),
  );

  ngOnInit(): void {
    this.orderApi.findBranches().subscribe((branches) => {
      this.branches.set(branches);
      const firstBranch = branches[0]?.id ?? null;
      this.selectedBranchId.set(firstBranch);
      if (firstBranch === null) {
        this.loading.set(false);
        return;
      }
      this.loadTickets(false);
    });
    this.refreshSubscription = interval(15_000).subscribe(() => {
      if (!this.loading() && !this.refreshing() && this.changingIds().size === 0) {
        this.loadTickets(true);
      }
    });
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
  }

  protected selectBranch(value: string): void {
    const branchId = Number(value);
    this.selectedBranchId.set(Number.isInteger(branchId) && branchId > 0 ? branchId : null);
    this.loadTickets(false);
  }

  protected selectDestination(destination: DestinationFilter): void {
    this.destination.set(destination);
    this.loadTickets(false);
  }

  protected toggleHistory(): void {
    this.showHistory.update((current) => !current);
    this.loadTickets(false);
  }

  protected refresh(): void {
    this.loadTickets(false);
  }

  protected itemsForStatus(
    ticket: PreparationTicket,
    status: PreparationStatus,
  ): PreparationItem[] {
    return ticket.items.filter((item) => item.status === status);
  }

  protected columnCount(status: PreparationStatus): number {
    return this.tickets().reduce(
      (total, ticket) => total + this.itemsForStatus(ticket, status).length,
      0,
    );
  }

  protected destinationLabel(ticket: PreparationTicket): string {
    return ticket.destination === 'PRODUCTION' ? 'Cocina' : 'Barra / servicio';
  }

  protected pointLabel(ticket: PreparationTicket): string {
    return ticket.order.servicePointName ?? ticket.order.customerReference ?? 'Para llevar';
  }

  protected elapsedLabel(sentAt: string): string {
    const elapsedMinutes = Math.max(
      0,
      Math.floor((Date.now() - new Date(sentAt).getTime()) / 60_000),
    );
    if (elapsedMinutes < 1) {
      return 'Ahora';
    }
    if (elapsedMinutes < 60) {
      return `${elapsedMinutes} min`;
    }
    const hours = Math.floor(elapsedMinutes / 60);
    const minutes = elapsedMinutes % 60;
    return `${hours} h ${minutes} min`;
  }

  protected isDelayed(sentAt: string): boolean {
    return Date.now() - new Date(sentAt).getTime() >= 15 * 60_000;
  }

  protected nextStatus(status: PreparationStatus): PreparationStatus | null {
    if (status === 'PENDING') {
      return 'IN_PREPARATION';
    }
    if (status === 'IN_PREPARATION') {
      return 'READY';
    }
    if (status === 'READY') {
      return 'DELIVERED';
    }
    return null;
  }

  protected actionLabel(status: PreparationStatus): string {
    if (status === 'PENDING') {
      return 'Comenzar';
    }
    if (status === 'IN_PREPARATION') {
      return 'Marcar lista';
    }
    return 'Entregada';
  }

  protected advance(item: PreparationItem): void {
    const next = this.nextStatus(item.status);
    if (next) {
      this.changeStatus(item, next);
    }
  }

  protected cancel(item: PreparationItem): void {
    this.confirmations.confirm({
      header: 'Cancelar partida',
      message: `¿Confirmas que ${item.quantity} × ${item.productName} no debe prepararse?`,
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: 'Conservar',
      acceptLabel: 'Cancelar partida',
      acceptButtonProps: { severity: 'danger' },
      accept: () => this.changeStatus(item, 'CANCELLED'),
    });
  }

  protected historyItems(ticket: PreparationTicket): PreparationItem[] {
    return ticket.items.filter((item) => item.status === 'DELIVERED' || item.status === 'CANCELLED');
  }

  private loadTickets(silent: boolean): void {
    const branchId = this.selectedBranchId();
    if (branchId === null) {
      this.tickets.set([]);
      this.loading.set(false);
      return;
    }
    if (!silent) {
      this.refreshing.set(true);
    }
    const selectedDestination = this.destination();
    const destination = selectedDestination === 'ALL' ? undefined : selectedDestination;
    this.preparationApi
      .findTickets(branchId, destination, !this.showHistory())
      .pipe(
        finalize(() => {
          this.loading.set(false);
          if (!silent) {
            this.refreshing.set(false);
          }
        }),
      )
      .subscribe((tickets) => this.tickets.set(tickets));
  }

  private changeStatus(item: PreparationItem, status: PreparationStatus): void {
    this.changingIds.update((current) => new Set(current).add(item.id));
    this.preparationApi
      .changeItemStatus(item.id, status, item.version)
      .pipe(
        finalize(() => {
          this.changingIds.update((current) => {
            const next = new Set(current);
            next.delete(item.id);
            return next;
          });
        }),
      )
      .subscribe((updatedTicket) => {
        const hasVisibleItems =
          this.showHistory() ||
          updatedTicket.items.some((candidate) =>
            ['PENDING', 'IN_PREPARATION', 'READY'].includes(candidate.status),
          );
        this.tickets.update((tickets) => {
          if (!hasVisibleItems) {
            return tickets.filter((ticket) => ticket.id !== updatedTicket.id);
          }
          return tickets.map((ticket) =>
            ticket.id === updatedTicket.id ? updatedTicket : ticket,
          );
        });
        this.messages.add({
          severity: status === 'CANCELLED' ? 'warn' : 'success',
          summary: status === 'CANCELLED' ? 'Partida cancelada' : 'Estado actualizado',
          detail: `${item.quantity} × ${item.productName}`,
        });
      });
  }
}
