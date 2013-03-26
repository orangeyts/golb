package rcp_01.actions;
import  org.eclipse.jface.action.Action;
import  org.eclipse.ui.IWorkbenchWindow;
import  org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import rcp_01.dialog.FirstDialog;


 public   class  Action1  extends  Action  implements  IWorkbenchAction  {
     
      private  IWorkbenchWindow workbenchWindow;

      public  Action1(IWorkbenchWindow window)  {
          if  (window  ==   null )  {
              throw   new  IllegalArgumentException();
         }

          this .workbenchWindow  =  window;
     }
     
      public   void  run()  {
          //  make sure action is not disposed
          if  (workbenchWindow  !=   null )  {
              // ���������ӹ���
             FirstDialog dg  =   new  FirstDialog(workbenchWindow.getShell());
             dg.open();
             
         }
     }

      public   void  dispose()  {
         workbenchWindow  =   null ;

     } 
}