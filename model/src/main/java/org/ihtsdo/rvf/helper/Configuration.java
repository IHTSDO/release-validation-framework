package org.ihtsdo.rvf.helper;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.ihtsdo.rvf.entity.ExecutionCommand;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * An object that holds configurable parameters that can be passed to model entities.
 * Interally it is just a collection of {@link org.ihtsdo.rvf.helper.ConfigurationItem}s
 */
@Embeddable
@Entity(name = "configuration")
public class Configuration {

    @Id
    @GeneratedValue
    private Long id;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "configuration")
    @JsonManagedReference
    Set<ConfigurationItem> items = new HashSet<>();
    @OneToOne(fetch = FetchType.EAGER, mappedBy = "configuration")
    @JsonBackReference
    ExecutionCommand command;

    public Set<ConfigurationItem> getItems() {
        return items;
    }

    public void setItems(Set<ConfigurationItem> items) {
        this.items.clear();
        for(ConfigurationItem item: items){
            item.setConfiguration(this);
            this.items.add(item);
        }
    }

    public boolean addItem(ConfigurationItem item){
        item.setConfiguration(this);
        return items.add(item);
    }

    public boolean removeItem(ConfigurationItem item){
        return items.remove(item);
    }

    public void clear(){
        items.clear();
    }

    @Transient
    public String getValue(String key){
        String value = null;
        for(ConfigurationItem item: items)
        {
            if(item.getKey().equals(key)){
                value = item.getValue();
                break;
            }
        }

        return value;
    }

    @Transient
    public Set<String> getKeys(){
        Set<String> keys = new HashSet<>();
        for(ConfigurationItem item : items)
        {
            keys.add(item.key);
        }

        return keys;
    }

    @Transient
    public boolean setValue(String key, String value){
        boolean foundMatch = false;
        for(ConfigurationItem item: items)
        {
            if(item.key.equals(key)){
                item.setConfiguration(this);
                item.value = value;
                foundMatch = true;
                break;
            }
        }

        // if no match found, then add a new item
        if(!foundMatch){
            foundMatch = items.add(new ConfigurationItem(key, value, false, this));
        }

        return foundMatch;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExecutionCommand getCommand() {
        return command;
    }

    public void setCommand(ExecutionCommand command) {
        this.command = command;
    }
}
