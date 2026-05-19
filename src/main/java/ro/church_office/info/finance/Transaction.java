package ro.church_office.info.finance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "financial_transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; private Long churchId; private Double amount; private LocalDate date; private String type; private String name; private String description;
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public Long getChurchId(){return churchId;} public void setChurchId(Long c){churchId=c;}
    public Double getAmount(){return amount;} public void setAmount(Double a){amount=a;} public LocalDate getDate(){return date;} public void setDate(LocalDate d){date=d;}
    public String getType(){return type;} public void setType(String t){type=t;} public String getName(){return name;} public void setName(String n){name=n;} public String getDescription(){return description;} public void setDescription(String d){description=d;}
}
