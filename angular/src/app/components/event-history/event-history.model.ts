export interface PaginatedEventsResponse {
  events: EventExpanded[];
  total: number;
  page: number;
  nextPage?: number | null;
  previousPage?: number | null;
}

export interface EventExpanded {
  event: string;
  timestamp: string;
  user: string;
  pipeline: string;
  bulk_update_uuid?: string | null;
  change_list?: string[] | null;
  event_type: string;
  asset_guid?: string | null;
}
