
package com.cibc.api.training.myaccounts.model;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {

    
    private String firstName;
    
    private String lastName;
    
    private String id;
    
    private String middleInitial;
    

    public Customer () {
    }

    
    
    @JsonProperty("firstName")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    
    
    @JsonProperty("lastName")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    
    
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    
    
    @JsonProperty("middleInitial")
    public String getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(String middleInitial) {
        this.middleInitial = middleInitial;
    }
    
    

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer customer = (Customer) o;

        return Objects.equals(firstName, customer.firstName) &&
        Objects.equals(lastName, customer.lastName) &&
        Objects.equals(id, customer.id) &&
        
        Objects.equals(middleInitial, customer.middleInitial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, id,  middleInitial);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Customer {\n");
        
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    middleInitial: ").append(toIndentedString(middleInitial)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
