package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.AssetGroup;
import dk.northtech.dasscoassetservice.domain.User;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.sql.Array;
import java.util.List;
import java.util.Optional;

public interface AssetGroupRepository extends SqlObject {

    // CREATE GROUP
    @Transaction
    default void createAssetGroup(AssetGroup assetGroup, User user) {
        withHandle(handle -> {

            // 1. Create group
            handle.createUpdate("""
                            INSERT INTO asset_group (group_name, creator_user_id)
                            VALUES (:group_name, :creator_user_id)
                            ON CONFLICT (group_name) DO NOTHING
                            """)
                    .bind("group_name", assetGroup.group_name)
                    .bind("creator_user_id", user.dassco_user_id)
                    .execute();

            // 2. Get asset_group_id
            Integer assetGroupId = handle.createQuery("""
                            SELECT asset_group_id
                            FROM asset_group
                            WHERE group_name = :group_name
                            """)
                    .bind("group_name", assetGroup.group_name)
                    .mapTo(Integer.class)
                    .one();

            // 3. Grant creator access
            handle.createUpdate("""
                            INSERT INTO asset_group_access (asset_group_id, dassco_user_id)
                            VALUES (:asset_group_id, :user_id)
                            ON CONFLICT DO NOTHING
                            """)
                    .bind("asset_group_id", assetGroupId)
                    .bind("user_id", user.dassco_user_id)
                    .execute();

            // 4. Add assets
            if (assetGroup.assets != null && !assetGroup.assets.isEmpty()) {
                var batch = handle.prepareBatch("""
                        INSERT INTO asset_group_asset (asset_group_id, asset_guid)
                        VALUES (:asset_group_id, :asset_guid)
                        ON CONFLICT DO NOTHING
                        """);
                for (String assetGuid : assetGroup.assets) {
                    batch.bind("asset_group_id", assetGroupId)
                            .bind("asset_guid", assetGuid)
                            .add();
                }
                batch.execute();
            }

            return null;
        });
    }

    //  GET GROUPS MADE BY THE USER
    default List<AssetGroup> readOwnedListAssetGroup(User user) {
        return withHandle(handle -> handle.createQuery("""
                        SELECT ag.group_name,
                               cu.username AS creator_username,
                               COALESCE(
                                   ARRAY_AGG(DISTINCT a.asset_guid)
                                   FILTER (WHERE a.asset_guid IS NOT NULL),
                                   '{}'
                               ) AS assets,
                               COALESCE(
                                   ARRAY_AGG(DISTINCT u.username)
                                   FILTER (WHERE u.username IS NOT NULL),
                                   '{}'
                               ) AS has_access
                        FROM asset_group ag
                                 JOIN dassco_user cu ON cu.dassco_user_id = ag.creator_user_id
                                 LEFT JOIN asset_group_asset aga ON aga.asset_group_id = ag.asset_group_id
                                 LEFT JOIN asset a ON a.asset_guid = aga.asset_guid
                                 LEFT JOIN asset_group_access acc ON acc.asset_group_id = ag.asset_group_id
                                 LEFT JOIN dassco_user u ON u.dassco_user_id = acc.dassco_user_id
                        WHERE cu.username = :username
                        GROUP BY ag.group_name, cu.username
                        """)
                .bind("username", user.username)
                .map((rs, ctx) -> {
                    AssetGroup group = new AssetGroup();
                    group.group_name = rs.getString("group_name");
                    group.groupCreator = rs.getString("creator_username");
                    group.assets =
                            List.of((String[]) rs.getArray("assets").getArray());
                    group.hasAccess =
                            List.of((String[]) rs.getArray("has_access").getArray());
                    return group;
                })
                .list());
    }

    // READ SINGLE GROUP
    default Optional<AssetGroup> readAssetGroup(String groupName) {
        return withHandle(handle -> handle.createQuery("""
                        SELECT ag.group_name,
                               cu.username       AS creator_username,
                               COALESCE(ARRAY_AGG(DISTINCT a.asset_guid)
                                        FILTER (WHERE a.asset_guid IS NOT NULL), '{}') AS assets,
                               COALESCE(ARRAY_AGG(DISTINCT u.username)
                                        FILTER (WHERE u.username IS NOT NULL), '{}') AS has_access
                        FROM asset_group ag
                                 JOIN dassco_user cu ON cu.dassco_user_id = ag.creator_user_id
                                 LEFT JOIN asset_group_asset aga ON aga.asset_group_id = ag.asset_group_id
                                 LEFT JOIN asset a ON a.asset_guid = aga.asset_guid
                                 LEFT JOIN asset_group_access acc ON acc.asset_group_id = ag.asset_group_id
                                 LEFT JOIN dassco_user u ON u.dassco_user_id = acc.dassco_user_id
                        WHERE ag.group_name = :group_name
                        GROUP BY ag.group_name, cu.username
                        """)
                .bind("group_name", groupName)
                .map((rs, ctx) -> {
                    AssetGroup g = new AssetGroup();
                    g.group_name = rs.getString("group_name");
                    g.groupCreator = rs.getString("creator_username");

                    String[] assets = (String[]) rs.getArray("assets").getArray();
                    g.assets = List.of(assets);

                    String[] users = (String[]) rs.getArray("has_access").getArray();
                    g.hasAccess = List.of(users);

                    return g;
                })
                .findOne());
    }

    default List<AssetGroup> readAssetGroupFromGroupNames(List<String> groupNames, User user) {
        if (groupNames == null || groupNames.isEmpty()) {
            return List.of();
        }

        return withHandle(handle -> handle.createQuery("""
                        SELECT ag.group_name,
                               cu.username       AS creator_username,
                               COALESCE(ARRAY_AGG(DISTINCT a.asset_guid)
                                        FILTER (WHERE a.asset_guid IS NOT NULL), '{}') AS assets,
                               COALESCE(ARRAY_AGG(DISTINCT u.username)
                                        FILTER (WHERE u.username IS NOT NULL), '{}') AS has_access
                        FROM asset_group ag
                                 JOIN dassco_user cu ON cu.dassco_user_id = ag.creator_user_id
                                 LEFT JOIN asset_group_asset aga ON aga.asset_group_id = ag.asset_group_id
                                 LEFT JOIN asset a ON a.asset_guid = aga.asset_guid
                                 LEFT JOIN asset_group_access acc ON acc.asset_group_id = ag.asset_group_id
                                 LEFT JOIN dassco_user u ON u.dassco_user_id = acc.dassco_user_id
                        WHERE ag.group_name IN (<groupNames>) AND cu.dassco_user_id = :userId
                        GROUP BY ag.group_name, cu.username
                        """)
                .bindList("groupNames", groupNames)
                .bind("userId", user.dassco_user_id)
                .map((rs, ctx) -> {
                    AssetGroup g = new AssetGroup();
                    g.group_name = rs.getString("group_name");
                    g.groupCreator = rs.getString("creator_username");

                    Array assetArray = rs.getArray("assets");
                    g.assets = assetArray != null
                            ? List.of((String[]) assetArray.getArray())
                            : List.of();

                    Array usersArray = rs.getArray("has_access");
                    g.hasAccess = usersArray != null
                            ? List.of((String[]) usersArray.getArray())
                            : List.of();

                    return g;
                })
                .list());
    }

    // READ USER ACCESSIBLE GROUPS
    default List<AssetGroup> readListAssetGroup(User user) {
        return withHandle(handle -> handle.createQuery("""
                        SELECT ag.group_name,
                               cu.username AS creator_username,
                               COALESCE(ARRAY_AGG(DISTINCT a.asset_guid)
                                        FILTER (WHERE a.asset_guid IS NOT NULL), '{}') AS assets,
                               COALESCE(ARRAY_AGG(DISTINCT u.username)
                                        FILTER (WHERE u.username IS NOT NULL), '{}') AS has_access
                        FROM asset_group ag
                                 JOIN dassco_user cu ON cu.dassco_user_id = ag.creator_user_id
                                 JOIN asset_group_access aga1 ON aga1.asset_group_id = ag.asset_group_id
                                 JOIN dassco_user viewer ON viewer.dassco_user_id = aga1.dassco_user_id
                                 LEFT JOIN asset_group_access acc ON acc.asset_group_id = ag.asset_group_id
                                 LEFT JOIN dassco_user u ON u.dassco_user_id = acc.dassco_user_id
                                 LEFT JOIN asset_group_asset aga ON aga.asset_group_id = ag.asset_group_id
                                 LEFT JOIN asset a ON a.asset_guid = aga.asset_guid
                        WHERE viewer.username = :username
                        GROUP BY ag.group_name, cu.username
                        """)
                .bind("username", user.username)
                .map((rs, ctx) -> {
                    AssetGroup g = new AssetGroup();
                    g.group_name = rs.getString("group_name");
                    g.groupCreator = rs.getString("creator_username");
                    g.assets = List.of((String[]) rs.getArray("assets").getArray());
                    g.hasAccess = List.of((String[]) rs.getArray("has_access").getArray());
                    return g;
                })
                .list());
    }

    // DELETE ASSET GROUP
    @Transaction
    default void deleteAssetGroup(String groupName) {
        withHandle(handle -> {
            handle.createUpdate("""
                            DELETE FROM asset_group WHERE group_name = :group_name
                            """)
                    .bind("group_name", groupName)
                    .execute();
            return null;
        });
    }

    // DELETE ASSET GROUPS
    @Transaction
    default boolean deleteAssetGroups(List<String> groupNames, User user) {
        return withHandle(handle -> handle.createUpdate("""
                        DELETE FROM asset_group WHERE group_name in (<groupNames>) AND creator_user_id = :userId
                        """)
                .bindList("groupNames", groupNames)
                .bind("userId", user.dassco_user_id)
                .execute() > 0);
    }

    // ADD ASSETS TO GROUP
    @Transaction
    default Optional<AssetGroup> addAssetsToAssetGroup(List<String> assets, String groupName) {
        withHandle(handle -> {
            Integer agId = handle.createQuery("""
                            SELECT asset_group_id FROM asset_group WHERE group_name = :group_name
                            """)
                    .bind("group_name", groupName)
                    .mapTo(Integer.class)
                    .one();

            var batch = handle.prepareBatch("""
                    INSERT INTO asset_group_asset (asset_group_id, asset_guid)
                    VALUES (:asset_group_id, :asset_guid)
                    ON CONFLICT DO NOTHING
                    """);
            for (String guid : assets) {
                batch.bind("asset_group_id", agId)
                        .bind("asset_guid", guid)
                        .add();
            }
            batch.execute();
            return null;
        });
        return readAssetGroup(groupName);
    }

    @Transaction
    default Optional<AssetGroup> removeAssetsFromAssetGroup(List<String> assets, String groupName) {
        withHandle(handle -> {
            handle.createUpdate("""
                            DELETE FROM asset_group_asset
                            WHERE asset_group_id = (
                                SELECT asset_group_id FROM asset_group WHERE group_name = :group_name
                            )
                              AND asset_guid IN (<assets>)
                            """)
                    .bind("group_name", groupName)
                    .bindList("assets", assets)
                    .execute();
            return null;
        });
        return readAssetGroup(groupName);
    }


    // GRANT / REVOKE ACCESS
    @Transaction
    default Optional<AssetGroup> grantAccessToAssetGroup(List<String> usernames, String groupName) {
        withHandle(handle -> {
            Integer groupId = handle.createQuery("""
                            SELECT asset_group_id FROM asset_group WHERE group_name = :group_name
                            """)
                    .bind("group_name", groupName)
                    .mapTo(Integer.class)
                    .one();

            var batch = handle.prepareBatch("""
                    INSERT INTO asset_group_access (asset_group_id, dassco_user_id)
                    VALUES (:group_id,
                            (SELECT dassco_user_id FROM dassco_user WHERE username = :username))
                    ON CONFLICT DO NOTHING
                    """);
            for (String u : usernames) {
                batch.bind("group_id", groupId)
                        .bind("username", u)
                        .add();
            }
            batch.execute();
            return null;
        });
        return readAssetGroup(groupName);
    }

    @Transaction
    default Optional<AssetGroup> revokeAccessToAssetGroup(List<String> usernames, String groupName) {
        withHandle(handle -> {
            handle.createUpdate("""
                            DELETE FROM asset_group_access
                            WHERE asset_group_id = (
                                SELECT asset_group_id FROM asset_group WHERE group_name = :group_name
                            )
                              AND dassco_user_id IN (
                                SELECT dassco_user_id FROM dassco_user WHERE username IN (<usernames>)
                              )
                            """)
                    .bind("group_name", groupName)
                    .bindList("usernames", usernames)
                    .execute();
            return null;
        });
        return readAssetGroup(groupName);
    }

    // GET ACCESS LIST
    default List<String> getHasAccess(String groupName) {
        return withHandle(handle -> handle.createQuery("""
                        SELECT du.username
                        FROM asset_group_access a
                                 JOIN asset_group ag ON ag.asset_group_id = a.asset_group_id
                                 JOIN dassco_user du ON du.dassco_user_id = a.dassco_user_id
                        WHERE ag.group_name = :group_name
                        """)
                .bind("group_name", groupName)
                .mapTo(String.class)
                .list());
    }
}