package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.dao.EntityDao;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.helper.MissingEntityException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EntityServiceImpl<T> implements EntityService<T> {

	private final EntityDao dao;
    private ReleaseCenter ihtsdo;

	public EntityServiceImpl(EntityDao dao) {
		this.dao = dao;
	}

	@Override
	public T update(T entity) {
        dao.save(entity);
        return entity;
	}

	@Override
	public void delete(T entity) {
        dao.delete(entity);
	}

    @Override
    public T create(T entity) {
        dao.save(entity);
        return entity;
    }

    @Override
    public List<T> findAll(Class clazz) {
        return dao.findAll(clazz);
    }

    @Override
    public T find(Class clazz, Long id){
        T t = (T) dao.load(clazz, id);
        if (t == null){
            throw new MissingEntityException(id);
        }
        else{
            return t;
        }
    }

    @Override
    public T find(Class clazz, UUID uuid){
        T t = (T) dao.findByUuid(clazz, uuid);
        if (t == null){
            throw new MissingEntityException(uuid);
        }
        else{
            return t;
        }
    }

    @Override
    public Long count(Class clazz){
        return dao.count(clazz);
    }

    /**
     * Utility method that returns a singleton instance of IHTSDO as a release centre, configured with default values.
     * @return a singleton instance of IHTSDO as a release centre, configured with default values
     */
    @Override
    public ReleaseCenter getIhtsdo(){
        if(ihtsdo == null){
            ihtsdo = new ReleaseCenter();
            ihtsdo.setName("International Health Terminology Standards Development Organisation ");
            ihtsdo.setShortName("IHTSDO");
            ihtsdo.setInactivated(false);
            dao.save(ihtsdo);

            return ihtsdo;
        }
        else{
            return ihtsdo;
        }
    }
}
