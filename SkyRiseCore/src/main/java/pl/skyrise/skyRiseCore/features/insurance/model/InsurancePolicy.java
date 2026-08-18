package pl.skyrise.skyRiseCore.features.insurance.model;

public class InsurancePolicy {

  private long expiresAt;
  private int charges;
  private String effectId;

  public InsurancePolicy(long expiresAt, int charges, String effectId) {
    this.expiresAt = expiresAt;
    this.charges = Math.max(0, charges);
    this.effectId = effectId == null || effectId.isBlank() ? "default" : effectId.toLowerCase();
  }

  public long getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(long expiresAt) {
    this.expiresAt = expiresAt;
  }

  public int getCharges() {
    return charges;
  }

  public void setCharges(int charges) {
    this.charges = Math.max(0, charges);
  }

  public void addCharges(int amount) {
    this.charges = Math.max(0, this.charges + amount);
  }

  public boolean consumeCharge() {
    if (charges <= 0) return false;
    charges--;
    return true;
  }

  public String getEffectId() {
    return effectId;
  }

  public void setEffectId(String effectId) {
    this.effectId = effectId == null || effectId.isBlank() ? "default" : effectId.toLowerCase();
  }

  public boolean isActive(long now) {
    return charges > 0 && expiresAt > now;
  }
}
