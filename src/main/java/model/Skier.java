package model;


public class Skier {

  private Integer skierID;
  private Integer resortID;
  private String seasonID;
  private String dayID;
  private LiftRide liftRide;

  public Skier() {
  }

  public Skier(Integer skierID, Integer resortID, String seasonID, String dayID,
      LiftRide liftRide) {
    this.skierID = skierID;
    this.resortID = resortID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.liftRide = liftRide;
  }

  public Integer getSkierID() {
    return skierID;
  }

  public void setSkierID(Integer skierID) {
    this.skierID = skierID;
  }

  public Integer getResortID() {
    return resortID;
  }

  public void setResortID(Integer resortID) {
    this.resortID = resortID;
  }

  public String getSeasonID() {
    return seasonID;
  }

  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  public String getDayID() {
    return dayID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public LiftRide getLiftRide() {
    return liftRide;
  }

  public void setLiftRide(LiftRide liftRide) {
    this.liftRide = liftRide;
  }

  @Override
  public String toString() {
    return "Skier{" +
        "skierID=" + skierID +
        ", resortID=" + resortID +
        ", seasonID='" + seasonID + '\'' +
        ", dayID='" + dayID + '\'' +
        ", liftRide=" +liftRide.toString()+
        '}';
  }
}
