
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;

public class ViewerFactory
{
   public static final String REGISTERED_VIEWERS = "registered.viewers";
   public static final String REGISTERED_VIEWERS_DEF
      = "TemperatureViewer,HumidityViewer,SwitchViewer,PotentiometerViewer,"
      + "ADViewer,ClockViewer,MemoryViewer,FileViewer,SHA18Viewer,SHA33Viewer,"
      + "ThermochronViewer,MissionViewer";

   private ContainerViewer containerViewer = null;
   private Class[] viewerClass = null;
   private Viewer[] viewerObject = null;
   /* A place for logging all error messages */
   private MessageLog log = null;

   public ViewerFactory(MessageLog log)
   {
      this.log = log;
      this.containerViewer = new ContainerViewer();
      this.containerViewer.setLogger(log);
      // create the class array
      this.viewerClass = new Class[12];

      // get the string of registered viewers to create, and tokenize
      String strViewers = ViewerProperties.getProperty(REGISTERED_VIEWERS,
                                                       REGISTERED_VIEWERS_DEF);
      StringTokenizer strtok = new StringTokenizer(strViewers, ", ", false);
      int total = 0;
      while(strtok.hasMoreTokens())
      {
         // check to see if we need to grow the array
         int max = this.viewerClass.length;
         if(total >= max)
         {
            // grow the array size if necessary
            Class[] temp = new Class[max + 10];
            System.arraycopy(this.viewerClass, 0, temp, 0, max);
            this.viewerClass = temp;
         }

         // get the class string for loading the viewer
         String className = (String)strtok.nextToken();

         // get the class object for the viewer
         try
         {
            this.viewerClass[total] = Class.forName(className);
         }
         catch(ClassNotFoundException cnfe)
         {
            System.err.println("couldn't load " + className);
            cnfe.printStackTrace();
            continue;
         }

         // check for duplicate entry
         boolean duplicate = false;
         for(int i=0; !duplicate && i<total; i++)
            duplicate = (viewerClass[i]==viewerClass[total]);

         if(!duplicate)
         {
            // increment total number of items
            total += 1;
         }
      }

      // trim the viewer class array
      if(total<viewerClass.length)
      {
         Class[] temp = new Class[total];
         System.arraycopy(this.viewerClass, 0, temp, 0, total);
         this.viewerClass = temp;
      }

      // create the viewer objects array
      this.viewerObject = new Viewer[total];
      for(int i=0; i<total; i++)
      {
         try
         {
            this.viewerObject[i] = (Viewer)this.viewerClass[i].newInstance();
            this.viewerObject[i].setLogger(this.log);
            this.viewerObject[i].clearContainer();
         }
         catch(Exception e)
         {
            System.err.println("couldn't load " + this.viewerClass[i].getName());
            e.printStackTrace();
         }
      }
   }

   public boolean isDefaultViewer(Viewer v)
   {
      return (v==containerViewer || v.getClass().equals(containerViewer.getClass()));
   }

   public Enumeration getDefaultViewers()
   {
      // create a vector for holding all viewers
      Vector vec = new Vector(this.viewerObject.length+1);

      // add the default ContainerViewer
      this.containerViewer.clearContainer();
      vec.addElement(this.containerViewer);

      return vec.elements();
   }

   public Enumeration getViewers(OneWireContainer owc)
   {
      // create a vector for holding all viewers
      Vector vec = new Vector(this.viewerObject.length+1);

      if(owc!=null)
      {
         // add the default ContainerViewer
         this.containerViewer.setContainer(owc);
         vec.addElement(this.containerViewer);

         // add each applicable viewer to the list
         for(int i=0; i<this.viewerObject.length; i++)
         {
            try
            {
               if(this.viewerObject[i].containerSupported(owc))
               {
                  this.viewerObject[i].setContainer(owc);
                  vec.addElement(this.viewerObject[i]);
               }
            }
            catch(Exception e)
            {
               System.err.println("couldn't load "
                                  + this.viewerClass[i].getName());
               e.printStackTrace();
            }
         }
      }
      else
      {
         // add the default ContainerViewer
         this.containerViewer.clearContainer();
         vec.addElement(this.containerViewer);
      }

      return vec.elements();
   }

   public Enumeration getViewers(TaggedDevice td)
   {
      // create a vector for holding all viewers
      Vector vec = new Vector(this.viewerObject.length+1);

      if(td!=null)
      {
         // add the default ContainerViewer
         this.containerViewer.setContainer(td);
         vec.addElement(this.containerViewer);

         // add each applicable viewer to the list
         for(int i=0; i<this.viewerObject.length; i++)
         {
            try
            {
               if(this.viewerObject[i].containerSupported(td))
               {
                  this.viewerObject[i].setContainer(td);
                  vec.addElement(this.viewerObject[i]);
               }
            }
            catch(Exception e)
            {
               System.err.println("couldn't load "
                                  + this.viewerClass[i].getName());
               e.printStackTrace();
            }
         }
      }
      else
      {
         // add the default ContainerViewer
         this.containerViewer.clearContainer();
         vec.addElement(this.containerViewer);
      }

      return vec.elements();
   }

   public Enumeration getAllViewers()
   {
      // create a vector for holding all viewers
      Vector vec = new Vector(this.viewerObject.length+1);

      // add the default ContainerViewer
      vec.addElement(containerViewer);

      // add each applicable viewer to the list
      for(int i=0; i<this.viewerObject.length; i++)
      {
         vec.addElement(this.viewerObject[i]);
      }

      return vec.elements();
   }

   public Viewer getViewer(OneWireContainer owc, String viewerTitle)
   {
      return getViewer(owc, viewerTitle, false);
   }

   public Viewer getViewer(OneWireContainer owc, String viewerTitle,
                           boolean unique)
   {
      Viewer viewer = null;
      for(int i=0; i<this.viewerObject.length; i++)
      {
         String testTitle = this.viewerObject[i].getViewerTitle();
         if((testTitle==viewerTitle || testTitle.equals(viewerTitle)) &&
               this.viewerObject[i].containerSupported(owc))
         {
            if(unique && this.viewerObject[i].isCloneable())
            {
               try
               {
                  viewer = (Viewer)this.viewerClass[i].newInstance();
                  viewer.setLogger(log);
               }
               catch(Exception e)
               {
                  System.err.println("couldn't load "
                                     + this.viewerClass[i].getName());
                  e.printStackTrace();
               }
            }
            else
            {
               viewer = this.viewerObject[i];
            }
            viewer.setContainer(owc);
         }
      }

      return viewer;
   }


   public Viewer getViewer(TaggedDevice td, String viewerTitle)
   {
      return getViewer(td, viewerTitle, false);
   }

   public Viewer getViewer(TaggedDevice td, String viewerTitle,
                           boolean unique)
   {
      Viewer viewer = null;
      for(int i=0; i<this.viewerObject.length; i++)
      {
         String testTitle = this.viewerObject[i].getViewerTitle();
         if(testTitle==viewerTitle || testTitle.equals(viewerTitle) &&
               this.viewerObject[i].containerSupported(td))
         {
            if(unique && this.viewerObject[i].isCloneable())
            {
               try
               {
                  viewer = (Viewer)this.viewerClass[i].newInstance();
                  viewer.setLogger(log);
               }
               catch(Exception e)
               {
                  System.err.println("couldn't load "
                                     + this.viewerClass[i].getName());
                  e.printStackTrace();
               }
            }
            else
            {
               viewer = this.viewerObject[i];
            }
            viewer.setContainer(td);
         }
      }

      return viewer;
   }

   public void clearAllViewers()
   {
      for(int i=0; i<this.viewerObject.length; i++)
      {
         this.viewerObject[i].clearContainer();
      }
   }
}