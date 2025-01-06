package com.iniciativaselebi.afrikanaone;

public class ClassificationData {
        private int aciertos;
        private int puntuacion;
        private int ganancias;
        private String uii;

        public ClassificationData() {
        }

        public ClassificationData(int aciertos, int puntuacion, int ganancias, String uii) {
            this.aciertos = aciertos;
            this.puntuacion = puntuacion;
            this.ganancias = ganancias;
            this.uii = uii;
        }

        public int getAciertos() {
            return aciertos;
        }

        public void setAciertos(int aciertos) {
            this.aciertos = aciertos;
        }

        public int getPuntuacion() {
            return puntuacion;
        }

        public void setPuntuacion(int puntuacion) {
            this.puntuacion = puntuacion;
        }

        public int getGanancias() {
            return ganancias;
        }

        public void setGanancias(int ganancias) {
            this.ganancias = ganancias;
        }

        public String getUii() {
            return uii;
        }

        public void setUii(String uii) {
            this.uii = uii;
        }
    }


