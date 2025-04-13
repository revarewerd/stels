package ru.sosgps.wayrecall.initialization;

import org.eclipse.jetty.annotations.AnnotationParser;
import org.eclipse.jetty.annotations.ClassNameResolver;
import org.eclipse.jetty.util.MultiException;

import java.util.Set;

/**
* Created by nickl on 27.04.14.
*/
class MyAnnotationParser extends AnnotationParser {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyAnnotationParser.class);
    private static org.slf4j.Logger LOG =  log;

    protected void parseDir (Set<? extends Handler> handlers, org.eclipse.jetty.util.resource.Resource dir, ClassNameResolver resolver)
            throws Exception
    {
        //skip dirs whose name start with . (ie hidden)
        if (!dir.isDirectory() || !dir.exists() || dir.getName().startsWith("."))
            return;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning dir {}", dir);};

        MultiException me = new MultiException();

        String[] files=dir.list();
        for (int f=0;files!=null && f<files.length;f++)
        {
            org.eclipse.jetty.util.resource.Resource res = dir.addPath(files[f]);
            if (res.isDirectory())
                parseDir(handlers, res, resolver);
            else
            {

                String name2 = res.getName();
                boolean valid = name2 != null && name2.endsWith(".class");
                //log.debug("name1:"+name2+" is valid:"+valid);
                if (valid)
                {
                    try
                    {
                        String name = res.getName();
                        if ((resolver == null)|| (!resolver.isExcluded(name) && (!isParsed(name) || resolver.shouldOverride(name))))
                        {
                            org.eclipse.jetty.util.resource.Resource r = org.eclipse.jetty.util.resource.Resource.newResource(res.getURL());
                            //if (LOG.isDebugEnabled()) {
                                //LOG.debug("Scanning class {}", r);};
                            scanClass(handlers, dir, r.getInputStream());
                        }
                    }
                    catch (Exception ex)
                    {
                        me.add(new RuntimeException("Error scanning file "+files[f],ex));
                    }
                }
                else
                {
                    if (LOG.isDebugEnabled()) LOG.debug("Skipping scan on invalid file {}", res);
                }
            }
        }

        me.ifExceptionThrow();
    }


}
