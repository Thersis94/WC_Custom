package com.ram.persistance;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/****************************************************************************
 * <b>Title:</b> RAMCasePersistanceFactory.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 3, 2017
 ****************************************************************************/
public class RAMCasePersistanceFactory {
	private RAMCasePersistanceFactory() {
		//Hide public Constructor for Factories.
	}

	public enum PersistanceType {SESSION(RAMCaseSessionPersist.class), DB(RAMCaseDBPersist.class);
		private Class<? extends PersistanceIntfc<?, ?>> classNm;
		private Class<? extends Object> sourceType;
		PersistanceType(Class<? extends PersistanceIntfc<?, ?>> classNm) {
			this.classNm = classNm;

			//Get the SourceType Generic Parameter.
			ParameterizedType parameterizedType = (ParameterizedType) classNm.getGenericInterfaces()[0];
		    Type[] typeArguments = parameterizedType.getActualTypeArguments();
		    sourceType = (Class<?>) typeArguments[0];
		}

		public Class<? extends Object> getSourceType() {
			return sourceType;
		}

		public Class<? extends PersistanceIntfc<?, ?>> getClassNm() {
			return classNm;
		}
	}

	@SuppressWarnings("unchecked")
	public static AbstractPersist<?, ?> loadPersistanceObject(PersistanceType pt, Object source) throws Exception {
		AbstractPersist<Object, Object> pi = null;
		Class<? extends Object> sourceType = pt != null ? pt.getSourceType() : null;
		if(pt != null && source != null && source.getClass().isInstance(sourceType)) {
			Class<?> c = pt.getClassNm();
			if (c == null) {
				throw new Exception("unknown persistance type:" + pt.toString());
			}

			//instantiate the action & return it - pass attributes & dbConn
			try {
				pi = (AbstractPersist<Object, Object>) c.newInstance();
				pi.setPersistanceSource(pt.getSourceType().cast(source));
			} catch(Exception e) {
				throw new Exception("unable to load persistance type:" + pt.toString());
			}
		}

		return pi;
	}
}
