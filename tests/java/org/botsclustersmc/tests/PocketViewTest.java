package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;
import org.botsclustersmc.core.Stack;
import java.util.*;

public final class PocketViewTest {
    private static int checks;
    private static void check(boolean value) {checks++;if(!value)throw new AssertionError("check "+checks);}
    public static void main(String[] args) {
        Pocket pocket=new Pocket();PocketView closed=PocketView.capture(pocket,Pocket.NONE);
        check(closed.cells().isEmpty()&&closed.cursor().empty()&&closed.output().empty());
        pocket.setStorage(0,new Stack("OAK_LOG",1));pocket.open(Pocket.Menu.INVENTORY);
        pocket.click(1,0,Pocket.NONE);PocketView carrying=PocketView.capture(pocket,Pocket.NONE);
        check(carrying.cursor().equals(new Stack("OAK_LOG",1))&&carrying.cells().size()==4);
        pocket.click(1,36,Pocket.NONE);PocketView prepared=PocketView.capture(pocket,Pocket.NONE);
        check(prepared.output().equals(new Stack("OAK_PLANKS",4)));
        check(prepared.cells().get(0).equals(new Stack("OAK_LOG",1)));
        pocket.click(3,40,Pocket.NONE);check(pocket.count("OAK_PLANKS")==4);
        check(prepared.output().equals(new Stack("OAK_PLANKS",4))&&!prepared.cells().get(0).empty());
        check(carrying.cursor().equals(new Stack("OAK_LOG",1)));
        try {prepared.cells().set(0,Stack.EMPTY);throw new AssertionError("mutable view");}
        catch(UnsupportedOperationException expected){checks++;}
        check(prepared.contents().contains("36=OAK_LOG x1")&&prepared.summary().contains("output=OAK_PLANKS x4"));
        for(Pocket.Menu menu:Pocket.Menu.values()) {
            Pocket p=new Pocket();p.open(menu);String before=p.menu()+":"+p.cursor()+":"+p.crafted;
            PocketView view=PocketView.capture(p,Pocket.NONE);
            int count=switch(menu){case CLOSED->0;case INVENTORY->4;case WORKBENCH->9;case FURNACE->3;case CHEST->27;};
            check(view.cells().size()==count&&view.menu()==menu);
            check(before.equals(p.menu()+":"+p.cursor()+":"+p.crafted));
            p.close();check(view.menu()==menu);
        }
        try {new PocketView(Pocket.Menu.WORKBENCH,Stack.EMPTY,Stack.EMPTY,List.of());throw new AssertionError("shape");}
        catch(IllegalArgumentException expected){checks++;}
        System.out.println("PASS immutable operator pocket view checks="+checks);
    }
}
