import {Moment} from "moment-timezone";

export enum AssetStatus {
  WORKING_COPY
  , ARCHIVE
  , BEING_PROCESSED
  , PROCESSING_HALTED
  , ISSUE_WITH_MEDIA
  , ISSUE_WITH_METADATA
  , FOR_DELETION
}

export interface Asset {
  asset_pid: string | undefined;
  asset_guid: string | undefined;
  status: AssetStatus | undefined;
  multi_specimen: boolean | undefined;
  specimens: Specimen[] | undefined;
  funding: string | undefined;
  subject: string | undefined;
  payload_type: string | undefined;
  file_formats: FileFormat[] | undefined;
  asset_locked: boolean | undefined;
  restricted_access: string[] | undefined;
  tags: Map<string, string> | undefined;
  audited: boolean | undefined;
  created_date: Moment | undefined;
  date_metadata_updated: Moment | undefined;
  date_asset_taken: Moment | undefined;
  date_asset_deleted: Moment | undefined;
  date_asset_finalised: Moment | undefined;
  date_metadata_taken: Moment | undefined;
  institution: string | undefined;
  parent_guid: string | undefined;
  collection: string | undefined;
  httpInfo: string | undefined;
  internal_status: string | undefined;
  updateUser: string | undefined;
  events: Event[] | undefined;
  digitiser: string | undefined;
  workstation: string | undefined;
  pipeline: string | undefined;
  error_message: string | undefined;
  error_timestamp: Moment | undefined;
}

export interface Specimen {
  institution: string | undefined;
  collection: string | undefined;
  barcode: string | undefined;
  specimen_pid: string | undefined;
  preparation_type: string | undefined;
}

export interface Event {
  user: string | undefined;
  timeStamp: Moment | undefined;
  event: string | undefined;
  pipeline: string | undefined;
  workstation: string | undefined;
}

export interface AssetGroup {
  group_name: string | undefined;
  assets: string[] | undefined;
  hasAccess: string[] | undefined;
}

export interface Digitiser {
  userId: string | undefined;
  name: string | undefined;
}

export enum FileFormat {
  TIF
  , JPEG
  , RAW
  , RAF
  , CR3
  , DNG
  , TXT
}
