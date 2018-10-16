//package org.ihtsdo.rvf.service;
//
//import java.util.List;
//import java.util.UUID;
//
//import org.ihtsdo.rvf.dao.EntityDao;
//import org.ihtsdo.rvf.helper.EntityNotFoundException;
//
//public class EntityServiceImpl<T> implements EntityService<T> {
//
//	private final EntityDao<T> dao;
//
//	public EntityServiceImpl(EntityDao<T> dao) {
//		this.dao = dao;
//	}
//
//	@Override
//	public T update(T entity) {
//		dao.update(entity);
//		return entity;
//	}
//
//	@Override
//	public void delete(T entity) {
//		dao.delete(entity);
//	}
//
//	@Override
//	public T create(T entity) {
//	   return dao.save(entity);
//	}
//
//	@Override
//	public List<T> findAll(Class<T> clazz) {
//		return dao.findAll(clazz);
//	}
//
//	@Override
//	public T find(Class<T> clazz, Long id){
//		T t = dao.load(clazz, id);
//		if (t == null){
//			throw new EntityNotFoundException(id);
//		}
//		else{
//			return t;
//		}
//	}
//
//	@Override
//	public T find(Class<T> clazz, UUID uuid){
//		T t = dao.findByUuid(clazz, uuid);
//		if (t == null){
//			throw new EntityNotFoundException(uuid);
//		}
//		else{
//			return t;
//		}
//	}
//
//	@Override
//	public Long count(Class<T> clazz){
//		return dao.count(clazz);
//	}
//}
