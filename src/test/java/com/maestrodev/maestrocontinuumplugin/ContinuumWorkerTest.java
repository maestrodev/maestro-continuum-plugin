package com.maestrodev.maestrocontinuumplugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.simple.JSONObject;

/**
 * Unit test for simple App.
 */
public class ContinuumWorkerTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ContinuumWorkerTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ContinuumWorkerTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testBuild() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        ContinuumWorker continuumWorker = new ContinuumWorker();
        JSONObject fields = new JSONObject();
        fields.put("project", "HelloWorld");
        fields.put("project_group", "com.maestrodev");        
        fields.put("goals", "clean test install package");
        fields.put("arguments", "--batch-mode");
        fields.put("host", "localhost");
        fields.put("port", 9292);
        fields.put("username", "admin");        
        fields.put("password", "adm1n");        
        fields.put("web_path", "/continuum");
        fields.put("timeout", 60);

        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);
        continuumWorker.setWorkitem(workitem);
               
        
        Method method = continuumWorker.getClass().getMethod("build");
        method.invoke(continuumWorker);
    }
}
