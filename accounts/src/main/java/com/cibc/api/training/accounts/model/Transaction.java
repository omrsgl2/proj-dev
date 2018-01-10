
package com.cibc.api.training.accounts.model;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {

    
    
    
    public enum TransactionTypeEnum {
        
        DEBIT ("DEBIT"),
        
        CREDIT ("CREDIT");
        

        private final String value;

        TransactionTypeEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static TransactionTypeEnum fromValue(String text) {
            for (TransactionTypeEnum b : TransactionTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                return b;
                }
            }
            return null;
        }
    }

    private TransactionTypeEnum transactionType;

    
    
    private String accountID;
    
    private double amount;
    
    private String id;
    

    public Transaction () {
    }

    
    
    @JsonProperty("transactionType")
    public TransactionTypeEnum getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionTypeEnum transactionType) {
        this.transactionType = transactionType;
    }
    
    
    
    @JsonProperty("accountID")
    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }
    
    
    
    @JsonProperty("amount")
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    
    
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Transaction transaction = (Transaction) o;

        return Objects.equals(transactionType, transaction.transactionType) &&
        Objects.equals(accountID, transaction.accountID) &&
        Objects.equals(amount, transaction.amount) &&
        
        Objects.equals(id, transaction.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionType, accountID, amount,  id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Transaction {\n");
        
        sb.append("    transactionType: ").append(toIndentedString(transactionType)).append("\n");
        sb.append("    accountID: ").append(toIndentedString(accountID)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
