package org.ihtsdo.rvf.helper;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * An object that holds configurable parameters that can be passed to model entities.
 * Interally it is just a collection of {@link org.ihtsdo.rvf.helper.ConfigurationItem}s
 */
@Entity
public class Configuration {

    @Id
    @GeneratedValue
    private Long id;
    @Embedded
    @ElementCollection(targetClass = ConfigurationItem.class)
    Set<ConfigurationItem> items = new HashSet<>();

    public Set<ConfigurationItem> getItems() {
        return items;
    }

    public void setItems(Set<ConfigurationItem> items) {
        this.items = items;
    }

    @Transient
    public boolean addItem(ConfigurationItem item){
        return items.add(item);
    }

    @Transient
    public boolean removeItem(ConfigurationItem item){
        return items.remove(item);
    }

    @Transient
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
                item.value = value;
                foundMatch = true;
                break;
            }
        }

        // if no match found, then add a new item
        if(!foundMatch){
            foundMatch = items.add(new ConfigurationItem(key, value, false));
        }

        return foundMatch;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
