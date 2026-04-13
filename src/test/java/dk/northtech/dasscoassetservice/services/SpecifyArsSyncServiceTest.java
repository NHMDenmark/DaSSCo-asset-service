package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifyArsSyncMessage;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifySyncStatus;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncAcknowledge;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncParkingSpaceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpecifyArsSyncServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SpecifyArsSyncService specifyArsSyncService;

    @MockitoBean
    FileProxyClient fileProxyClient;

    private User syncUser;

    @BeforeEach
    void setup() {
        syncUser = userService.ensureExists(new User("specify-sync-test-user"));
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
                        && "Asset is locked, but have parked files".equals(ack.additional_info())));
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
                        && ack.additional_info() == null));
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
                        && req.specifySyncLogId.equals(1003L)));
        verify(queueBroadcaster, never()).sendSpecifyArsAcknowledge(any(SyncAcknowledge.class));
    }

    @Test
    void newAssetWithoutParkedFilesSendsSucceededAcknowledge() {
        Asset newAsset = AssetServiceTest.getTestAsset("specify-sync-new-no-files");

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(newAsset, new HashSet<>(), 1004L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(queueBroadcaster).sendSpecifyArsAcknowledge(argThat(ack ->
                ack.specifySyncStatus() == SpecifySyncStatus.SUCCEEDED
                        && ack.specifySyncLogId().equals(1004L)
                        && ack.additional_info() == null));
        verify(fileProxyClient, never()).syncParkedFile(any(SyncParkingSpaceRequest.class));
    }

    @Test
    void newAssetWithParkedFilesSyncsParkingSpace() {
        Asset newAsset = AssetServiceTest.getTestAsset("specify-sync-new-with-files");
        insertParkedFile(newAsset.institution, newAsset.collection, newAsset.asset_guid, "image.jpg");
        when(fileProxyClient.syncParkedFile(any(SyncParkingSpaceRequest.class))).thenReturn(SpecifySyncStatus.STARTED);

        clearInvocations(queueBroadcaster, fileProxyClient);
        SpecifyArsSyncMessage message = new SpecifyArsSyncMessage(newAsset, new HashSet<>(), 1005L);

        specifyArsSyncService.handleSpecifyUpdate(message);

        verify(fileProxyClient).syncParkedFile(argThat(req ->
                req.asset.asset_guid().equals(newAsset.asset_guid)
                        && req.asset.institution().equals(newAsset.institution)
                        && req.asset.collection().equals(newAsset.collection)
                        && req.specifySyncLogId.equals(1005L)));
        verify(queueBroadcaster, never()).sendSpecifyArsAcknowledge(any(SyncAcknowledge.class));
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
