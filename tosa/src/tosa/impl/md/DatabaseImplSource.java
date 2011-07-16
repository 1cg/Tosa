package tosa.impl.md;

import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.module.IModule;
import tosa.api.IDatabase;
import tosa.dbmd.DatabaseImpl;
import tosa.loader.data.DBData;
import tosa.loader.data.IDBDataSource;
import tosa.loader.parser.DDLDBDataSource;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: alan
 * Date: 6/28/11
 * Time: 9:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImplSource {

  // TODO - AHK - Maybe rename this to TosaMetadataService?

  // TODO - AHK - This should be a singleton service

  private Map<IModule, Collection<? extends IDatabase>> _dbsByModule;
  private Map<String, IDatabase> _dbsByName;

  public IDatabase getDatabase(String namespace) {
    maybeLoadDBData();
    return _dbsByName.get(namespace);
  }

  public Collection<? extends IDatabase> getAllDatabases() {
    maybeLoadDBData();
    return _dbsByName.values();
  }

  public Collection<? extends IDatabase> getAllDatabasesForModule(IModule module) {
    maybeLoadDBData();
    return _dbsByModule.get(module);
  }

  // TODO - AHK - Need a hook to reload the db data

  private void maybeLoadDBData() {
    // TODO - AHK - Synchronization
    if (_dbsByModule == null) {
      _dbsByModule = new HashMap<IModule, Collection<? extends IDatabase>>();
      _dbsByName = new HashMap<String, IDatabase>();

      // TODO - AHK - What if the type system isn't initialized?
      List<? extends IModule> modules = TypeSystem.getExecutionEnvironment().getModules();
      for (IModule module : modules) {
        // TODO - AHK - This seems a bit ugly
        List<IDatabase> dbs = new ArrayList<IDatabase>();
        IDBDataSource dataSource = new DDLDBDataSource();
        Map<String, DBData> dbDataMap = dataSource.getDBData(module);
        for (Map.Entry<String, DBData> dbDataEntry : dbDataMap.entrySet()) {
          IDatabase database = new DatabaseImpl(dbDataEntry.getKey(), dbDataEntry.getValue(), module);
          // TODO - AHK - Validate things!
          dbs.add(database);
          _dbsByName.put(database.getNamespace(), database);
        }
        _dbsByModule.put(module, dbs);
      }
    }
  }
}