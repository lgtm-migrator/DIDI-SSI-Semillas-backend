package com.atixlabs.semillasmiddleware.app.model.credential;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;

@Getter
@Setter
@NoArgsConstructor
@Entity
@PrimaryKeyJoinColumn(referencedColumnName="id")
public class CredentialDwelling extends Credential {

    private String dwellingType;
    private String dwellingAddress;
    private String possessionType;
    //TODO campo que faltaba respuesta: "Distrito de Residencia" String
    private Boolean brick; //ladrillo
    private Boolean lock; //chapa
    private Boolean wood; //madera
    private Boolean paperBoard; //carton
    private String district; //distrito de residencia
    private String lightInstallation; //instalacion de luz
    private String generalConditions; //condiciones generales
    private String neighborhoodType; //tipo de barrio
    private Boolean gas; //red de gas
    private Boolean carafe; //garrafa
    private Boolean water; //red de agua
    private Boolean watterWell; //pozo / bomba
    private Integer antiquity; //antiguedad
    private Integer numberOfEnvironments; //cantidad de ambientes
    private Long rental; //monto alquiler
    private String address;
    private String location;
    private String neighborhood;

    public CredentialDwelling(CredentialDwelling credentialDwelling){
        super(credentialDwelling);
        this.dwellingType = credentialDwelling.dwellingType;
        this.dwellingAddress = credentialDwelling.dwellingAddress;
        this.possessionType = credentialDwelling.possessionType;
        //campos nuevos
        this.district = credentialDwelling.district;
        this.generalConditions = credentialDwelling.generalConditions;
        this.lightInstallation = credentialDwelling.lightInstallation;
        this.neighborhoodType = credentialDwelling.neighborhoodType;
        this.gas = credentialDwelling.gas;
        this.carafe = credentialDwelling.carafe;
        this.water = credentialDwelling.water;
        this.address = credentialDwelling.address;
        this.location = credentialDwelling.location;
        this.neighborhood = credentialDwelling.neighborhood;

    }

}
