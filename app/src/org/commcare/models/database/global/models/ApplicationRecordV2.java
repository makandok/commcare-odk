/**
 *
 */
package org.commcare.models.database.global.models;

import org.commcare.models.framework.Persisted;
import org.commcare.models.framework.Persisting;
import org.commcare.models.framework.Table;
import org.commcare.modern.models.MetaField;
import org.commcare.suite.model.Profile;

/**
 * An Application Record tracks an individual CommCare app on the current
 * install.
 *
 * @author ctsims
 * @author amstone
 */

@Table(ApplicationRecord.STORAGE_KEY)
public class ApplicationRecordV2 extends Persisted {

    private static final String META_STATUS = "status";

    @Persisting(1)
    protected String applicationId;
    @Persisting(2)
    @MetaField(META_STATUS)
    protected int status;
    @Persisting(3)
    protected String uniqueId;
    @Persisting(4)
    protected String displayName;
    @Persisting(5)
    protected boolean resourcesValidated;
    @Persisting(6)
    protected boolean isArchived;
    @Persisting(7)
    protected boolean convertedViaDbUpgrader;
    @Persisting(8)
    protected boolean preMultipleAppsProfile;
    @Persisting(9)
    protected int versionNumber;

    public ApplicationRecordV2(String applicationId, int status) {
        this.applicationId = applicationId;
        this.status = status;
    }

    /**
     * Deserialization only
     */
    public ApplicationRecordV2() {

    }

    public String getApplicationId() {
        return applicationId;
    }

    public int getStatus() {
        return status;
    }

    public void setResourcesStatus(boolean b) {
        this.resourcesValidated = b;
    }

    public void setArchiveStatus(boolean b) {
        this.isArchived = b;
    }

    public void setUniqueId(String s) {
        this.uniqueId = s;
    }

    public void setDisplayName(String s) {
        this.displayName = s;
    }

    public void setVersionNumber(int version) {
        this.versionNumber = version;
    }

    public void setConvertedByDbUpgrader(boolean b) {
        this.convertedViaDbUpgrader = b;
    }

    public void setPreMultipleAppsProfile(boolean b) {
        this.preMultipleAppsProfile = b;
    }



}