package tosa.impl;

import gw.util.GosuExceptionUtil;
import tosa.api.EntityCollection;
import tosa.api.IDBColumn;
import tosa.api.IDBObject;
import tosa.api.IPreparedStatementParameter;
import tosa.loader.DBTypeInfo;
import tosa.loader.IDBType;
import tosa.loader.data.DBColumnTypeImpl;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: alan
 * Date: 5/8/11
 * Time: 3:11 PM
 * To change this template use File | Settings | File Templates.
 */
// TODO - AHK - This name pretty much sucks
public class ReverseFkEntityCollectionImpl<T extends IDBObject> implements EntityCollection<T> {

  private IDBObject _owner;
  private IDBType _fkType;
  private IDBColumn _fkColumn;
  private QueryExecutor _queryExecutor;
  private List<T> _cachedResults;

  public ReverseFkEntityCollectionImpl(IDBObject owner, IDBType fkType, IDBColumn fkColumn, QueryExecutor queryExecutor) {
    _owner = owner;
    _fkType = fkType;
    _fkColumn = fkColumn;
    _queryExecutor = queryExecutor;
  }

  @Override
  public int size() {
    if (_cachedResults == null) {
      String text = SimpleSqlBuilder.select("count(*) as count").from(_fkType).where(_fkColumn, "=", "?").toString();
      IPreparedStatementParameter param = _fkColumn.wrapParameterValue(_owner.getColumnValue(DBTypeInfo.ID_COLUMN));
      return _queryExecutor.count("ReverseFkEntityCollectionImpl.size()", text, param);
    } else {
      return _cachedResults.size();
    }
  }

  @Override
  public Iterator<T> iterator() {
    loadResultsIfNecessary();
    return new EntityCollectionImplIterator(_cachedResults.iterator());
  }

  @Override
  public T get(int index) {
    loadResultsIfNecessary();
    if (index < 0 || index > _cachedResults.size() - 1) {
      throw new IndexOutOfBoundsException("Index " + index + " is invalid for an ReverseFkEntityCollectionImpl of size " + _cachedResults.size());
    }
    return _cachedResults.get(index);
  }

  @Override
  public void add(T element) {
    if (!_fkType.isAssignableFrom(element.getIntrinsicType())) {
      throw new IllegalArgumentException("An element of type " + element.getIntrinsicType() + " cannot be added to a collection of type " + _fkType);
    }

    if (_owner.isNew()) {
      throw new IllegalStateException("The element cannot be added to the list, as the owner is not yet committed.  You must commit the owner prior to added anything to the list.");
    }

    Object existingId = element.getColumnValue(_fkColumn.getName());
    if (existingId == null) {
      // We always set the back-pointer and the column
      element.setFkValue(_fkColumn.getName(), _owner);
      if (element.isNew()) {
        // For newly-created elements, we insert them immediately
        try {
          element.update();
        } catch (SQLException e) {
          GosuExceptionUtil.forceThrow(e);
        }
      } else {
        // For entities already in the database, we issue the update statement in the database directly
        IDBColumn idColumn = _fkColumn.getTable().getColumn(DBTypeInfo.ID_COLUMN);
        String updateSql = SimpleSqlBuilder.update(_fkColumn.getTable()).set(_fkColumn, "?").where(idColumn, "=", "?").toString();
        IPreparedStatementParameter fkParam = idColumn.wrapParameterValue(_owner.getColumnValue(DBTypeInfo.ID_COLUMN));
        IPreparedStatementParameter idParam = idColumn.wrapParameterValue(element.getColumnValue(DBTypeInfo.ID_COLUMN));
        _queryExecutor.update("ReverseFkEntityCollectionImpl.add()", updateSql, fkParam, idParam);
      }
      if (_cachedResults != null) {
        // TODO - AHK - Unclear if the list should be re-sorted, or if it should be added in insertion order
        _cachedResults.add(element);
      }
    } else if (existingId.equals(_owner.getColumnValue(DBTypeInfo.ID_COLUMN))) {
      // That's fine, it's a no-op, but we still want to set the fk value so you get the right pointer back when you reference the fk
      element.setFkValue(_fkColumn.getName(), _owner);
    } else {
      throw new IllegalArgumentException("The element with id " + element.getColumnValue(DBTypeInfo.ID_COLUMN) + " is already attached to another owner, with id " + existingId);
    }
  }

  @Override
  public void remove(T element) {
    if (!_fkType.isAssignableFrom(element.getIntrinsicType())) {
      throw new IllegalArgumentException("An element of type " + element.getIntrinsicType() + " cannot be added to a collection of type " + _fkType);
    }

    if (_owner.isNew()) {
      throw new IllegalStateException("Elements cannot be removed from an entity that has not yet been persisted");
    }

    Object fkId = element.getColumnValue(_fkColumn.getName());
    if (!_owner.getId().equals(fkId)) {
      throw new IllegalArgumentException("The element with id " + element.getId() + " is not a member of the array on element " + _owner.getId());
    }

    element.setFkValue(_fkColumn.getName(), null);
    IDBColumn idColumn = _fkColumn.getTable().getColumn(DBTypeInfo.ID_COLUMN);
    String updateSql = SimpleSqlBuilder.update(_fkColumn.getTable()).set(_fkColumn, "NULL").where(idColumn, "=", "?").toString();
    IPreparedStatementParameter idParam = idColumn.wrapParameterValue(element.getColumnValue(DBTypeInfo.ID_COLUMN));
    _queryExecutor.update("ReverseFkEntityCollectionImpl.remove()", updateSql, idParam);

    if (_cachedResults != null) {
      // The _cachedResults might contain a different pointer, so we have to match up by id
      for (int i = 0; i < _cachedResults.size(); i++) {
        if (element.getId().equals(_cachedResults.get(i).getId())) {
          _cachedResults.remove(i);
          break;
        }
      }
    }
  }

  @Override
  public void load() {
    loadResultsIfNecessary();
  }

  @Override
  public void unload() {
    _cachedResults = null;
  }

  private void loadResultsIfNecessary() {
    if (_cachedResults == null) {
      IDBColumn idColumn = _fkColumn.getTable().getColumn(DBTypeInfo.ID_COLUMN);
      String sql = SimpleSqlBuilder.select("*").from(_fkColumn.getTable()).where(_fkColumn, "=", "?").order_by(idColumn).toString();
      IPreparedStatementParameter param = _fkColumn.wrapParameterValue(_owner.getColumnValue(DBTypeInfo.ID_COLUMN));
      _cachedResults = (List<T>) _queryExecutor.selectEntity("ReverseFkEntityCollectionImpl.loadResultsIfNecessary()", _fkType, sql, param);
    }
  }

  private class EntityCollectionImplIterator implements Iterator<T> {
    Iterator<T> _wrappedIterator;

    private EntityCollectionImplIterator(Iterator<T> wrappedIterator) {
      _wrappedIterator = wrappedIterator;
    }

    @Override
    public boolean hasNext() {
      return _wrappedIterator.hasNext();
    }

    @Override
    public T next() {
      return _wrappedIterator.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}