package com.mockrunner.mock.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mockrunner.util.ArrayUtil;
import com.mockrunner.util.ParameterUtil;
import com.mockrunner.util.SearchUtil;

/**
 * Abstract base class for all statement types
 * that support parameters, i.e. <code>PreparedStatement</code>
 * and <code>CallableStatement</code>.
 */
abstract class AbstractParameterResultSetHandler extends AbstractResultSetHandler
{
    private boolean exactMatchParameter = false;
    private Map resultSetsForStatement = new HashMap();
    private Map updateCountForStatement = new HashMap();
    
    /**
     * Sets if the specified parameters must match exactly
     * in order and number.
     * Defaults to <code>false</code>, i.e. the spcified
     * parameters must be present in the actual parameter
     * list of the prepared statement with the correct index
     * but it's ok if there are more actual parameters.
     * @param exactMatchParameter must parameters match exactly
     */
    public void setExactMatchParameter(boolean exactMatchParameter)
    {
        this.exactMatchParameter = exactMatchParameter;
    }

    /**
     * Returns the first update count that matches the
     * specified SQL string and the specified parameters. 
     * Please note that you can modify the search parameters with 
     * {@link #setCaseSensitive} and {@link #setExactMatch}. 
     * Returns <code>null</code> if no return value is present
     * for the specified SQL string.
     * @param sql the SQL string
     * @param parameters the parameters
     * @return the corresponding update count
     */
    public Integer getUpdateCount(String sql, Map parameters)
    {
        List list = SearchUtil.getMatchingObjects(updateCountForStatement, sql, getCaseSensitive(), getExactMatch(), true);
        for(int ii = 0; ii < list.size(); ii++)
        {
            MockUpdateCountWrapper wrapper = (MockUpdateCountWrapper)list.get(ii);
            if(doParameterMatch(wrapper.getParamters(), parameters))
            {
                return wrapper.getUpdateCount();
            }
        }
        return null;
    }

    /**
     * Returns the first <code>ResultSet</code> that matches the
     * specified SQL string and the specified parameters.
     * Please note that you can modify the search parameters for 
     * the SQL string with {@link #setCaseSensitive} and
     * {@link #setExactMatch} and the search parameters for the 
     * specified parameter list with {@link #setExactMatchParameter}.
     * @param sql the SQL string
     * @param parameters the parameters
     * @return the corresponding {@link MockResultSet}
     */
    public MockResultSet getResultSet(String sql, Map parameters)
    {
        List list = SearchUtil.getMatchingObjects(resultSetsForStatement, sql, getCaseSensitive(), getExactMatch(), true);
        for(int ii = 0; ii < list.size(); ii++)
        {
            MockResultSetWrapper wrapper = (MockResultSetWrapper)list.get(ii);
            if(doParameterMatch(wrapper.getParamters(), parameters))
            {
                return wrapper.getResultSet();
            }
        }
        return null;
    }

    private boolean doParameterMatch(Map expectedParameters, Map actualParameters)
    {
        if(exactMatchParameter)
        {
            if(actualParameters.size() != expectedParameters.size()) return false;
            Iterator iterator = actualParameters.keySet().iterator();
            while(iterator.hasNext())
            {
                Object currentKey = iterator.next();
                Object expectedObject = expectedParameters.get(currentKey);
                if(null == expectedObject) return false;
                if(!ParameterUtil.compareParameter(actualParameters.get(currentKey), expectedObject))
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            Iterator iterator = expectedParameters.keySet().iterator();
            while(iterator.hasNext())
            {
                Object currentKey = iterator.next();
                Object actualObject = actualParameters.get(currentKey);
                if(null == actualObject) return false;
                if(!ParameterUtil.compareParameter(actualObject, expectedParameters.get(currentKey)))
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Clears the <code>ResultSet</code> objects.
     */
    public void clearResultSets()
    {
        super.clearResultSets();
        resultSetsForStatement.clear();
    }
    
    /**
     * Clears the update counts.
     */
    public void clearUpdateCounts()
    {
        super.clearUpdateCounts();
        updateCountForStatement.clear();
    }

    /**
     * Prepare a <code>ResultSet</code> for a specified SQL string and
     * the specified parameters. The specified parameters array
     * must contain the parameters in the correct order starting with 0 as
     * the first parameter. Please keep in mind that parameters in
     * <code>PreparedStatement</code> objects start with 1 as the first
     * parameter. So <code>parameters[0]</code> maps to the
     * parameter with index 1.
     * @param sql the SQL string
     * @param paramters the parameters
     * @param resultSet the corresponding {@link MockResultSet}
     */
    public void prepareResultSet(String sql, MockResultSet resultSet, Object[] parameters)
    {
        prepareResultSet(sql, resultSet, ArrayUtil.getListFromObjectArray(parameters));
    }

    /**
     * Prepare a <code>ResultSet</code> for a specified SQL string and
     * the specified parameters. The specified parameters <code>List</code>
     * must contain the parameters in the correct order starting with 0 as
     * the first parameter. Please keep in mind that parameters in
     * <code>PreparedStatement</code> objects start with 1 as the first
     * parameter. So <code>parameters.get(0)</code> maps to the
     * parameter with index 1.
     * @param sql the SQL string
     * @param paramters the parameters
     * @param resultSet the corresponding {@link MockResultSet}
     */
    public void prepareResultSet(String sql, MockResultSet resultSet, List parameters)
    {
        Map params = new HashMap();
        for(int ii = 0; ii < parameters.size(); ii++)
        {
            params.put(new Integer(ii + 1), parameters.get(ii));
        }
        prepareResultSet(sql, resultSet, params);
    }
    
    /**
     * Prepare a <code>ResultSet</code> for a specified SQL string and
     * the specified parameters. The specified parameters <code>Map</code>
     * must contain the parameters by mapping <code>Integer</code> objects
     * to the corresponding parameter. The <code>Integer</code> object
     * is the index of the parameter. In the case of a <code>CallableStatement</code>
     * there are allowed also <code>String</code> keys for named parameters.
     * @param sql the SQL string
     * @param paramters the parameters
     * @param resultSet the corresponding {@link MockResultSet}
     */
    public void prepareResultSet(String sql, MockResultSet resultSet, Map parameters)
    {
        List list = (List)resultSetsForStatement.get(sql);
        if(null == list)
        {
            list = new ArrayList();
            resultSetsForStatement.put(sql, list);
        }
        list.add(new MockResultSetWrapper(resultSet, parameters));
    }

    /**
     * Prepare the update count for execute update calls for a specified SQL string
     * and the specified parameters. The specified parameters array
     * must contain the parameters in the correct order starting with 0 as
     * the first parameter. Please keep in mind that parameters in
     * <code>PreparedStatement</code> objects start with 1 as the first
     * parameter. So <code>parameters[0]</code> maps to the
     * parameter with index 1.
     * @param sql the SQL string
     * @param updateCount the update count
     * @param paramters the parameters
     */
    public void prepareUpdateCount(String sql, int updateCount, Object[] parameters)
    {
        prepareUpdateCount(sql, updateCount, ArrayUtil.getListFromObjectArray(parameters));
    }

    /**
     * Prepare the update count for execute update calls for a specified SQL string
     * and the specified parameters. The specified parameters <code>List</code>
     * must contain the parameters in the correct order starting with 0 as
     * the first parameter. Please keep in mind that parameters in
     * <code>PreparedStatement</code> objects start with 1 as the first
     * parameter. So <code>parameters.get(0)</code> maps to the
     * parameter with index 1.
     * @param sql the SQL string
     * @param updateCount the update count
     * @param paramters the parameters
     */
    public void prepareUpdateCount(String sql, int updateCount, List parameters)
    {
        Map params = new HashMap();
        for(int ii = 0; ii < parameters.size(); ii++)
        {
            params.put(new Integer(ii + 1), parameters.get(ii));
        }
        prepareUpdateCount(sql, updateCount,  params);
    }
    
    /**
     * Prepare the update count for execute update calls for a specified SQL string
     * and the specified parameters. The specified parameters <code>Map</code>
     * must contain the parameters by mapping <code>Integer</code> objects
     * to the corresponding parameter. The <code>Integer</code> object
     * is the index of the parameter. In the case of a <code>CallableStatement</code>
     * there are allowed also <code>String</code> keys for named parameters.
     * @param sql the SQL string
     * @param paramters the parameters
     * @param resultSet the corresponding {@link MockResultSet}
     */
    public void prepareUpdateCount(String sql, int updateCount, Map parameters)
    {
        List list = (List)updateCountForStatement.get(sql);
        if(null == list)
        {
            list = new ArrayList();
            updateCountForStatement.put(sql, list);
        }
        list.add(new MockUpdateCountWrapper(updateCount, parameters));
    }
    
    private class MockResultSetWrapper
    {
        private MockResultSet resultSet;
        private Map parameters;
    
        public MockResultSetWrapper(MockResultSet resultSet, Map parameters)
        {
            this.resultSet = resultSet;
            this.parameters = parameters;
        }
    
        public Map getParamters()
        {
            return parameters;
        }

        public MockResultSet getResultSet()
        {
            return resultSet;
        }
    }

    private class MockUpdateCountWrapper
    {
        private Integer updateCount;
        private Map parameters;

        public MockUpdateCountWrapper(int updateCount, Map parameters)
        {
            this.updateCount = new Integer(updateCount);
            this.parameters = parameters;
        }

        public Map getParamters()
        {
            return parameters;
        }

        public Integer getUpdateCount()
        {
            return updateCount;
        }
    }
}
