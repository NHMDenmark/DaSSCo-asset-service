package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetSpecimen;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifyArsSyncMessage;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifySyncStatus;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncAcknowledge;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncParkingSpaceRequest;
import dk.northtech.dasscoassetservice.repositories.SpecimenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpecifyArsSyncServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SpecifyArsSyncService specifyArsSyncService;

    @MockitoBean
    FileProxyClient fileProxyClient;

    @MockitoBean
    IngestionClient ingestionClient;

    private User syncUser;

    @BeforeEach
    void setup() {
        syncUser = userService.ensureExists(new User("specify-sync-test-user"));
        when(ingestionClient.generateGuid(any())).thenAnswer(invocation -> "generated-guid-" + UUID.randomUUID());
    }

    @Test
    void existingLockedAssetWithParkedFilesSendsFailedAcknowledge() {
        Asset existingAsset = AssetServiceTest.getTestAsset("specify-sync-locked-asset");
        existingAsset.asset_locked = true;
        assetService.persistAsset(existingAsset, syncUser, 1);
        insertParkedFile(existingAsset.institution, existingAsset.collection, existingAsset.asset_guid, "image.jpg");

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(existingAsset, Set.of(), 1001L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.FAILED
                        && ack.specifySyncLogId().equals(1001L)
                        && "Asset is locked, but have parked files".equals(ack.additionalInfo())));
        verify(fileProxyClient, never()).syncParkedFile(any(SyncParkingSpaceRequest.class));
    }

    @Test
    void existingAssetWithoutParkedFilesSendsSucceededAcknowledge() {
        Asset existingAsset = AssetServiceTest.getTestAsset("specify-sync-no-files-existing");
        existingAsset.asset_locked = false;
        assetService.persistAsset(existingAsset, syncUser, 1);

        clearInvocations(queueBroadcaster, fileProxyClient);
        Asset updatePayload = AssetServiceTest.getTestAsset(existingAsset.asset_guid);
        updatePayload.institution = existingAsset.institution;
        updatePayload.collection = existingAsset.collection;
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(updatePayload, Set.of(), 1002L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.SUCCEEDED
                        && ack.specifySyncLogId().equals(1002L)
                        && ack.additionalInfo() == null));
        verify(fileProxyClient, never()).syncParkedFile(any(SyncParkingSpaceRequest.class));
    }

    @Test
    void existingUnlockedAssetWithParkedFilesSyncsParkingSpace() {
        Asset existingAsset = AssetServiceTest.getTestAsset("specify-sync-existing-files");
        existingAsset.asset_locked = false;
        assetService.persistAsset(existingAsset, syncUser, 1);
        insertParkedFile(existingAsset.institution, existingAsset.collection, existingAsset.asset_guid, "image.jpg");
        when(fileProxyClient.syncParkedFile(any(SyncParkingSpaceRequest.class))).thenReturn(SpecifySyncStatus.STARTED);

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(existingAsset, Set.of(), 1003L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(fileProxyClient).syncParkedFile(argThat(req ->
                req.asset.asset_guid().equals(existingAsset.asset_guid)
                        && req.asset.institution().equals(existingAsset.institution)
                        && req.asset.collection().equals(existingAsset.collection)
                        && req.specifySyncLogId.equals(1003L)
                        && req.attachmentLocation.equals(existingAsset.asset_guid)));
        verify(queueBroadcaster, never()).sendSpecifyArsAcknowledge(any(SyncAcknowledge.class));
    }

    @Test
    void newAssetWithoutParkedFilesSendsSucceededAcknowledge() {
        Asset newAsset = AssetServiceTest.getTestAsset("specify-sync-new-no-files");

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(newAsset, new HashSet<>(), 1004L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.FAILED
                        && ack.specifySyncLogId().equals(1004L)
                        && "No parked files found for asset from Specify; asset not created".equals(ack.additionalInfo())
                        && ack.assetGuid().equals(newAsset.asset_guid)));
        verify(fileProxyClient, never()).syncParkedFile(any(SyncParkingSpaceRequest.class));
        verify(ingestionClient, never()).generateGuid(any());

        assertThat(assetService.getAsset(newAsset.asset_guid).isPresent()).isFalse();
    }

    @Test
    void newAssetWithParkedFilesSyncsParkingSpace() {
        Asset newAsset = AssetServiceTest.getTestAsset("specify-sync-new-with-files");
        String temporaryAssetGuid = newAsset.asset_guid;
        insertParkedFile(newAsset.institution, newAsset.collection, temporaryAssetGuid, "image.jpg");
        when(fileProxyClient.syncParkedFile(any(SyncParkingSpaceRequest.class))).thenReturn(SpecifySyncStatus.STARTED);

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(newAsset, new HashSet<>(), 1005L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(fileProxyClient).syncParkedFile(argThat(req ->
                req.asset.asset_guid().startsWith("generated-guid-")
                        && req.asset.institution().equals(newAsset.institution)
                        && req.asset.collection().equals(newAsset.collection)
                        && req.specifySyncLogId.equals(1005L)
                        && req.attachmentLocation.equals(temporaryAssetGuid)));
        verify(queueBroadcaster, never()).sendSpecifyArsAcknowledge(any(SyncAcknowledge.class));
        verify(ingestionClient, times(1)).generateGuid(any());
    }

    @Test
    void newAssetWithParkedFilesAndNon202ResponseRollsBackAndSendsFailedAcknowledge() {
        Asset newAsset = AssetServiceTest.getTestAsset("specify-sync-new-non-202");
        String temporaryAssetGuid = newAsset.asset_guid;
        insertParkedFile(newAsset.institution, newAsset.collection, temporaryAssetGuid, "image.jpg");
        when(fileProxyClient.syncParkedFile(any(SyncParkingSpaceRequest.class))).thenReturn(SpecifySyncStatus.SUCCEEDED);

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(newAsset, new HashSet<>(), 1008L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.FAILED
                        && ack.specifySyncLogId().equals(1008L)
                        && ack.assetGuid().startsWith("generated-guid-")
                        && ack.additionalInfo() != null
                        && ack.additionalInfo().contains("expected HTTP 202")));

        assertThat(assetService.getAsset(message.asset.asset_guid).isPresent()).isFalse();
    }

    @Test
    void newAssetWithParkedFilesAndSyncExceptionRollsBackAndSendsFailedAcknowledge() {
        Asset newAsset = AssetServiceTest.getTestAsset("specify-sync-new-sync-exception");
        String temporaryAssetGuid = newAsset.asset_guid;
        insertParkedFile(newAsset.institution, newAsset.collection, temporaryAssetGuid, "image.jpg");
        when(fileProxyClient.syncParkedFile(any(SyncParkingSpaceRequest.class))).thenThrow(new RuntimeException("boom"));

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(newAsset, new HashSet<>(), 1009L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.FAILED
                        && ack.specifySyncLogId().equals(1009L)
                        && ack.assetGuid().startsWith("generated-guid-")
                        && ack.additionalInfo() != null
                        && ack.additionalInfo().contains("Failed to sync parked files for new asset")));

        assertThat(assetService.getAsset(message.asset.asset_guid).isPresent()).isFalse();
    }

    @Test
    void updatesExistingAssetFoundByCollectionObjectAttachmentIdWhenGuidDoesNotExist() {
        Asset existingAsset = AssetServiceTest.getTestAsset("specify-sync-find-by-coa-existing");
        AssetSpecimen existingSpecimen = new AssetSpecimen(existingAsset.asset_guid,
                "specify-sync-find-by-coa-existing-pid", "slide", false);
        existingSpecimen.specimen = specimenService.putSpecimen(new Specimen(
                existingAsset.institution,
                existingAsset.collection,
                "specify-sync-find-by-coa-existing-barcode",
                existingSpecimen.specimen_pid,
                new HashSet<>(List.of("slide")),
                null,
                null,
                new ArrayList<>()
        ), syncUser);
        existingSpecimen.specimen_id = existingSpecimen.specimen.specimen_id();
        existingAsset.asset_specimen.add(existingSpecimen);
        assetService.persistAsset(existingAsset, syncUser, 1);

        Asset persistedExisting = assetService.getAsset(existingAsset.asset_guid).orElseThrow();
        AssetSpecimen persistedSpecimen = persistedExisting.asset_specimen.get(0);
        Long collectionObjectAttachmentId = 33333L;
        jdbi.onDemand(SpecimenRepository.class).updateAssetSpecimen(
                persistedExisting.asset_guid,
                persistedSpecimen.specimen_id,
                collectionObjectAttachmentId,
                persistedSpecimen.asset_preparation_type
        );

        clearInvocations(queueBroadcaster, fileProxyClient);
        Asset updatePayload = AssetServiceTest.getTestAsset("specify-sync-find-by-coa-missing-guid");
        updatePayload.asset_subject = "changed-via-specify-sync";
        AssetSpecimen payloadSpecimen = new AssetSpecimen(updatePayload.asset_guid,
                persistedSpecimen.specimen_pid,
                persistedSpecimen.asset_preparation_type,
                false);
        payloadSpecimen.specify_collection_object_attachment_id = collectionObjectAttachmentId;
        payloadSpecimen.specimen = persistedSpecimen.specimen;
        payloadSpecimen.specimen_id = persistedSpecimen.specimen_id;
        updatePayload.asset_specimen.add(payloadSpecimen);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(updatePayload, Set.of("${payload_type}"), 1006L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        Optional<Asset> oldGuidAsset = assetService.getAsset(updatePayload.asset_guid);
        assertThat(oldGuidAsset.isPresent()).isFalse();
        Optional<Asset> updatedExistingAsset = assetService.getAsset(existingAsset.asset_guid);
        assertThat(updatedExistingAsset.isPresent()).isTrue();
        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.SUCCEEDED
                        && ack.specifySyncLogId().equals(1006L)
                        && ack.assetGuid().equals(existingAsset.asset_guid)));
    }

    @Test
    void multipleCollectionObjectAttachmentIdsOnDifferentAssetsSendsFailedAcknowledge() {
        Asset asset1 = createAssetWithSpecimen("specify-sync-coa-conflict-1", "specify-sync-coa-conflict-pid-1");
        Asset asset2 = createAssetWithSpecimen("specify-sync-coa-conflict-2", "specify-sync-coa-conflict-pid-2");

        Asset persisted1 = assetService.getAsset(asset1.asset_guid).orElseThrow();
        Asset persisted2 = assetService.getAsset(asset2.asset_guid).orElseThrow();
        jdbi.onDemand(SpecimenRepository.class).updateAssetSpecimen(
                persisted1.asset_guid,
                persisted1.asset_specimen.get(0).specimen_id,
                44441L,
                persisted1.asset_specimen.get(0).asset_preparation_type
        );
        jdbi.onDemand(SpecimenRepository.class).updateAssetSpecimen(
                persisted2.asset_guid,
                persisted2.asset_specimen.get(0).specimen_id,
                44442L,
                persisted2.asset_specimen.get(0).asset_preparation_type
        );

        clearInvocations(queueBroadcaster, fileProxyClient);
        Asset updatePayload = AssetServiceTest.getTestAsset("specify-sync-coa-conflict-missing-guid");
        AssetSpecimen payloadSpecimen1 = new AssetSpecimen(updatePayload.asset_guid, "specify-sync-coa-conflict-pid-1", "slide", false);
        payloadSpecimen1.specify_collection_object_attachment_id = 44441L;
        AssetSpecimen payloadSpecimen2 = new AssetSpecimen(updatePayload.asset_guid, "specify-sync-coa-conflict-pid-2", "slide", false);
        payloadSpecimen2.specify_collection_object_attachment_id = 44442L;
        updatePayload.asset_specimen.add(payloadSpecimen1);
        updatePayload.asset_specimen.add(payloadSpecimen2);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(updatePayload, Set.of(), 1007L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.FAILED
                        && ack.specifySyncLogId().equals(1007L)));
    }

    private Asset createAssetWithSpecimen(String assetGuid, String specimenPid) {
        Asset asset = AssetServiceTest.getTestAsset(assetGuid);
        AssetSpecimen specimen = new AssetSpecimen(asset.asset_guid, specimenPid, "slide", false);
        specimen.specimen = specimenService.putSpecimen(new Specimen(
                asset.institution,
                asset.collection,
                assetGuid + "-barcode",
                specimenPid,
                new HashSet<>(List.of("slide")),
                null,
                null,
                new ArrayList<>()
        ), syncUser);
        specimen.specimen_id = specimen.specimen.specimen_id();
        asset.asset_specimen.add(specimen);
        assetService.persistAsset(asset, syncUser, 1);
        return asset;
    }

    private void insertParkedFile(String institution, String collection, String assetGuid, String filename) {
        String path = institution + "/" + collection + "/" + assetGuid + "/" + filename;
        jdbi.withHandle(handle -> {
            handle.createUpdate("INSERT INTO parked_file(path, size_bytes, \"timestamp\") VALUES (:path, :size, now())")
                    .bind("path", path)
                    .bind("size", 123L)
                    .execute();
            return null;
        });
    }
}
