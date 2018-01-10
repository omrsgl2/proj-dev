
package com.cibc.api.training.accounts.model;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Account {

    
    private String customerID;
    
    private String id;
    
    private double balance;
    
    
    
    public enum AccountTypeEnum {
        
        DEPOSIT ("DEPOSIT"),
        
        SAVINGS ("SAVINGS"),
        
        CREDIT ("CREDIT"),
        
        MORTGAGE ("MORTGAGE"),
        
        LOC ("LOC");
        

        private final String value;

        AccountTypeEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static AccountTypeEnum fromValue(String text) {
            for (AccountTypeEnum b : AccountTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                return b;
                }
            }
            return null;
        }
    }

    private AccountTypeEnum accountType;

    
    

    public Account () {
    }

    
    
    @JsonProperty("customerID")
    public String getCustomerID() {
        return customerID;
    }

    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }
    
    
    
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    
    
    @JsonProperty("balance")
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    
    
    @JsonProperty("accountType")
    public AccountTypeEnum getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountTypeEnum accountType) {
        this.accountType = accountType;
    }
    
    

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Account account = (Account) o;

        return Objects.equals(customerID, account.customerID) &&
        Objects.equals(id, account.id) &&
        Objects.equals(balance, account.balance) &&
        
        Objects.equals(accountType, account.accountType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerID, id, balance,  accountType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Account {\n");
        
        sb.append("    customerID: ").append(toIndentedString(customerID)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    balance: ").append(toIndentedString(balance)).append("\n");
        sb.append("    accountType: ").append(toIndentedString(accountType)).append("\n");
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
