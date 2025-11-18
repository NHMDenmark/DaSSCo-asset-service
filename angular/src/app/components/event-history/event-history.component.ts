import {ChangeDetectionStrategy, Component, inject, OnDestroy} from '@angular/core';
import {EventHistoryService} from './event-history.service';
import {combineLatest, debounceTime, distinctUntilChanged, map, Subject, takeUntil} from 'rxjs';
import {DatePipe} from '@angular/common';
import {FormControl, FormGroup} from '@angular/forms';
import {MatSnackBar} from '@angular/material/snack-bar';
import {EventExpanded, PaginatedEventsResponse} from './event-history.model';

interface BulkUpdateGroupedEvent {
  event: EventExpanded;
  absoluteIndex: number;
  displayIndex: number;
}

interface BulkUpdateGroup {
  id: string;
  bulkUpdateUuid: string | null;
  events: BulkUpdateGroupedEvent[];
}

interface EventHistoryViewModel {
  eventsData: PaginatedEventsResponse;
  currentEventType: string;
  isBulkUpdateView: boolean;
  bulkUpdateGroups: BulkUpdateGroup[];
  limit: number;
}

@Component({
  selector: 'dassco-event-history',
  templateUrl: './event-history.component.html',
  styleUrls: ['./event-history.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DatePipe]
})
export class EventHistoryComponent implements OnDestroy {
  readonly eventHistoryService = inject(EventHistoryService);
  readonly datePipe = inject(DatePipe);
  readonly snackBar = inject(MatSnackBar);
  private readonly destroy$ = new Subject<void>();
  readonly limitOptions = [10, 25, 50, 100, 250, 500] as const;
  readonly eventHistoryViewModel$ = combineLatest([
    this.eventHistoryService.events$,
    this.eventHistoryService.eventType$,
    this.eventHistoryService.limit$
  ]).pipe(
    map(([eventsData, currentEventType, limit]): EventHistoryViewModel => {
      const isBulkUpdateView = currentEventType === 'BULK_UPDATE_ASSET_METADATA';
      return {
        eventsData,
        currentEventType,
        isBulkUpdateView,
        limit,
        bulkUpdateGroups: isBulkUpdateView
          ? this.groupEventsByBulkUpdate(eventsData.events, eventsData.page, limit)
          : []
      };
    })
  );

  dateRangeForm = new FormGroup({
    start: new FormControl<Date | null>(null),
    end: new FormControl<Date | null>(null)
  });
  limitControl = new FormControl<number>(100, {nonNullable: true});

  goToNextPage(nextPage: number | null | undefined) {
    if (nextPage !== null && nextPage !== undefined) {
      this.eventHistoryService.setPage(nextPage);
    }
  }

  goToPreviousPage(previousPage: number | null | undefined) {
    if (previousPage !== null && previousPage !== undefined) {
      this.eventHistoryService.setPage(previousPage);
    }
  }

  changeDirection(direction: 'ASC' | 'DESC') {
    this.eventHistoryService.setDirection(direction);
  }

  changeEventType(eventType: string) {
    this.eventHistoryService.setEventType(eventType);
  }

  getEventIndex(page: number, limit: number, arrayIndex: number): number {
    return (page - 1) * limit + arrayIndex + 1;
  }

  trackBulkUpdateGroup(_index: number, group: BulkUpdateGroup): string {
    return group.id;
  }

  trackGroupedEvent(_index: number, groupedEvent: BulkUpdateGroupedEvent): number {
    return groupedEvent.absoluteIndex;
  }

  constructor() {
    this.dateRangeForm.valueChanges.pipe(takeUntil(this.destroy$), debounceTime(100)).subscribe((range) => {
      if (range.start === null && range.end === null) {
        this.eventHistoryService.setStartDate(undefined);
        this.eventHistoryService.setEndDate(undefined);
        return;
      }
      if (range.start === null || range.end === null) {
        this.snackBar.open('Please select both start and end dates', 'OK', {
          duration: 5000,
          verticalPosition: 'top'
        });
        return;
      }
      this.eventHistoryService.setStartDate(range.start);
      this.eventHistoryService.setEndDate(range.end);
    });

    this.eventHistoryService.limit$.pipe(takeUntil(this.destroy$)).subscribe((limit) => {
      if (limit !== this.limitControl.value) {
        this.limitControl.setValue(limit, {emitEvent: false});
      }
    });

    this.limitControl.valueChanges.pipe(takeUntil(this.destroy$), distinctUntilChanged()).subscribe((limit) => {
      this.eventHistoryService.setLimit(limit);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  clearDateRange(): void {
    this.dateRangeForm.patchValue({
      start: null,
      end: null
    });
  }

  private groupEventsByBulkUpdate(events: EventExpanded[], page: number, limit: number): BulkUpdateGroup[] {
    const groups: BulkUpdateGroup[] = [];
    const groupLookup = new Map<string, BulkUpdateGroup>();

    events.forEach((event, absoluteIndex) => {
      const key = event.bulk_update_uuid ?? '__NO_BULK_UPDATE_UUID__';
      let group = groupLookup.get(key);
      if (!group) {
        group = {
          id: key,
          bulkUpdateUuid: event.bulk_update_uuid ?? null,
          events: []
        };
        groupLookup.set(key, group);
        groups.push(group);
      }
      group.events.push({
        event,
        absoluteIndex,
        displayIndex: this.getEventIndex(page, limit, absoluteIndex)
      });
    });

    return groups;
  }
}
