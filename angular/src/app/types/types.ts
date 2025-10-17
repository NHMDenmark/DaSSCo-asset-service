import {Moment} from 'moment-timezone';

export enum AssetStatus {
  WORKING_COPY,
  ARCHIVE,
  BEING_PROCESSED,
  PROCESSING_HALTED,
  ISSUE_WITH_MEDIA,
  ISSUE_WITH_METADATA,
  FOR_DELETION
}

export interface Asset {
  asset_pid: string | undefined;
  asset_guid: string | undefined;
  status: AssetStatus | undefined;
  multi_specimen: boolean | undefined;
  asset_specimen: AssetSpecimen[] | undefined;
  asset_subject?: string | undefined;
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
  parent_guids: string[] | undefined;
  collection: string | undefined;
  httpInfo: string | undefined;
  internal_status: string | undefined;
  updateUser: string | undefined;
  events: Event[] | undefined;
  digitiser: string | undefined;
  workstation: string | undefined;
  pipeline: string | undefined;
  error_message: string | undefined;
  error_timestamp: string | undefined;
  writeAccess: boolean | undefined;
}

export interface AssetSpecimen {
  specimen_id: number | undefined;
  asset_guid: string | undefined;
  specimen_pid: string | undefined;
  asset_specimen_id: number | undefined;
  asset_preparation_type: string | undefined;
  specify_collection_object_attachment_id: number | undefined;
  asset_detached: boolean | undefined;
  specimen: Specimen[] | undefined;
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
  timestamp: string | undefined;
  event: string | undefined;
  pipeline: string | undefined;
  workstation: string | undefined;
}

export interface AssetGroup {
  group_name: string | undefined;
  assets: string[] | undefined;
  hasAccess: string[] | undefined;
  groupCreator: string | undefined;
  isCreator: boolean | undefined;
}

export interface Digitiser {
  userId: string | undefined;
  name: string | undefined;
}

export interface DasscoError {
  type: string | undefined;
  protocolVersion: string | undefined;
  errorCode: string | undefined;
  errorMessage: string | undefined;
  body: string | undefined;
}

export enum FileFormat {
  TIF,
  JPEG,
  RAW,
  RAF,
  CR3,
  DNG,
  TXT
}

export interface Legality {
  id?: number;
  copyright?: string;
  license?: string;
  credit?: string;
}

export interface PublicAssetMetadata {
  asset_guid?: string;
  asset_pid?: string;
  asset_subject?: string;
  audited?: boolean;

  barcode?: string[];

  camera_setting_control?: string;
  collection?: string;
  date_asset_deleted_ars?: string;
  date_asset_taken?: string;
  date_audited?: string;

  file_formats?: string[];

  funding?: string[];
  institution?: string;
  legality?: Legality;
  metadata_version?: string;

  mime_type?: string[];
  mos_id?: string;
  multi_specimen?: boolean;

  parent_guids?: string[];
  payload_type?: string;
  pipeline_name?: string;
  preparation_type?: string[];

  specify_attachment_title?: string;
  specimen_pid?: string[];
}
