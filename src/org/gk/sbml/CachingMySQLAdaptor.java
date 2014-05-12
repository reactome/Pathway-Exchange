/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/**
 * Extends MySQLAdaptor to provide local caching of instances.
 * 
 * @author David Croft
 *
 */
public class CachingMySQLAdaptor extends MySQLAdaptor {
	Map<Long,GKInstance> instanceCache = new HashMap<Long,GKInstance>();
	
	public CachingMySQLAdaptor(String hostname, String dbName, String username, String password) throws SQLException {
		super(hostname, dbName, username, password);
	}

	public CachingMySQLAdaptor(String hostname, String dbName, String username, String password, int parseInt) throws SQLException {
		super(hostname, dbName, username, password, parseInt);
	}

//	public void addInstanceToCache(GKInstance instance) {
//		if (instance == null)
//			return;
//		instanceCache.put(instance.getDBID(), instance);
//	}
//
//	@Override
//	public GKInstance fetchInstance(Long dbID) throws Exception {
//		GKInstance instance = super.fetchInstance(dbID);
//		
//		if (instance == null)
//			instance = instanceCache.get(dbID);
//		
//		return instance;
//	}
	
}
