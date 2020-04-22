package com.atixlabs.semillasmiddleware.app.model.beneficiary;

import com.atixlabs.semillasmiddleware.app.model.credential.Credential;
import com.atixlabs.semillasmiddleware.excelparser.app.categories.PersonCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table
public class Person {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    private String documentType; //TODO enum or class

    @Column(unique = true)
    private Long documentNumber;

    private String name;

    private LocalDate birthDate;
    
    @JoinColumn(name = "ID_CREDENTIAL")
    @OneToMany
    private List<Credential> credentials;

    public Person(PersonCategory personCategory){
        this.setName(personCategory.getName()+" "+ personCategory.getSurname());
        this.setDocumentType(personCategory.getIdType());
        this.setDocumentNumber(personCategory.getIdNumber());
        this.setBirthDate(personCategory.getBirthDate());
    }


    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", documentType='" + documentType + '\'' +
                ", documentNumber=" + documentNumber +
                ", name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", credentials=" + credentials +
                '}';
    }

    /*
    kinsman (pariente), p1,p2, tiporelacion (kind of kinship)

    tipo de relacion
            hijo
    conyugue
            familiar*/
}
