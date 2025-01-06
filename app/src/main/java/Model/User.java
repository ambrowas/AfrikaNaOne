package Model;

public class User {

    public String fullname, email, telefono, ciudad, pais, profilePicture, deviceType, fechadeCreacion;
    public String uid;
    private int highestScore, accumulatedAciertos, accumulatedFallos, accumulatedPuntuacion, positionInLeaderboard;

    // New fields for flag and abbreviation
    private String flagUrl;
    private String countryAbbreviation;

    // No-argument constructor required by Firebase
    public User() {
        // Default values (if needed)
        this.deviceType = "Unknown";
        this.profilePicture = "";
    }

    public User(String nombre, String email, String telefono, String ciudad, String pais, String deviceType, String currentDate) {
        this.fullname = nombre;
        this.email = email;
        this.telefono = telefono;
        this.ciudad = ciudad;
        this.pais = pais;
        this.deviceType = deviceType;
        this.fechadeCreacion = currentDate;
    }

    public User(String fullname, String email, String telefono, String ciudad, String pais, String profilePicture, String deviceType, String fechadeCreacion) {
        this.fullname = fullname;
        this.email = email;
        this.telefono = telefono;
        this.ciudad = ciudad;
        this.pais = pais;
        this.profilePicture = profilePicture;
        this.deviceType = deviceType;
        this.fechadeCreacion = fechadeCreacion;
    }

    // Constructor without deviceType
    public User(String fullname, String email, String telefono, String ciudad, String pais, String profilePicture) {
        this.fullname = fullname;
        this.email = email;
        this.telefono = telefono;
        this.ciudad = ciudad;
        this.pais = pais;
        this.deviceType = "Unknown"; // Default value
        this.profilePicture = profilePicture;
    }

    // Getters and setters for flagUrl and countryAbbreviation
    public String getFlagUrl() {
        return flagUrl;
    }

    public void setFlagUrl(String flagUrl) {
        this.flagUrl = flagUrl;
    }

    public String getCountryAbbreviation() {
        return countryAbbreviation;
    }

    public void setCountryAbbreviation(String countryAbbreviation) {
        this.countryAbbreviation = countryAbbreviation;
    }


    // Getters and setters for all fields
    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    public int getAccumulatedAciertos() {
        return accumulatedAciertos;
    }

    public void setAccumulatedAciertos(int accumulatedAciertos) {
        this.accumulatedAciertos = accumulatedAciertos;
    }

    public int getAccumulatedFallos() {
        return accumulatedFallos;
    }

    public void setAccumulatedFallos(int accumulatedFallos) {
        this.accumulatedFallos = accumulatedFallos;
    }

    public int getAccumulatedPuntuacion() {
        return accumulatedPuntuacion;
    }

    public void setAccumulatedPuntuacion(int accumulatedPuntuacion) {
        this.accumulatedPuntuacion = accumulatedPuntuacion;
    }

    public int getPositionInLeaderboard() {
        return positionInLeaderboard;
    }

    public void setPositionInLeaderboard(int positionInLeaderboard) {
        this.positionInLeaderboard = positionInLeaderboard;
    }

    public void updatePositionInLeaderboard(int newPosition) {
        this.positionInLeaderboard = newPosition;
    }

    public void setFechadeCreacion(String fechadeCreacion) {
        this.fechadeCreacion = fechadeCreacion;
    }

    public String getFechadeCreacion() {
        return this.fechadeCreacion;
    }
}