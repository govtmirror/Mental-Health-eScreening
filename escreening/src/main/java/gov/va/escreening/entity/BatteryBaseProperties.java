package gov.va.escreening.entity;

/**
 * Created by pouncilt on 8/2/14.
 */
public interface BatteryBaseProperties {
    Integer getBatteryId();
    void setBatteryId(Integer batteryId);
    String getDescription();
    void setDescription(String description);
    boolean isDisabled();
    void setDisabled(boolean disabled);
}
