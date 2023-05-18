package Model;
public class User {

    public String fullname, email, telefono, ciudad, barrio, pais, profilePicture;
    public String uid;
    private int highestScore, accumulatedAciertos, accumulatedFallos, accumulatedPuntuacion, positionInLeaderboard;

    public User() {
    }

    public User(String fullname, String email, String telefono, String barrio, String ciudad, String pais, String profilePicture) {
        this.fullname = fullname;
        this.email = email;
        this.telefono = telefono;
        this.barrio = barrio;
        this.ciudad = ciudad;
        this.pais = pais;
        this.profilePicture = profilePicture;
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

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
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
}

