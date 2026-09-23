package org.botsclustersmc.tests;

import org.botsclustersmc.core.*;

public final class MenuFocusTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++;if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for(int operation=0;operation<6;operation++) {
            check(MenuFocus.active(true,operation),"an existing menu owns all inputs");
            check(MenuFocus.active(false,operation)==(operation==4),"opening the inventory takes focus immediately");
        }
        for(Task task:Task.values())for(int slots:new int[]{39,41,46,63}) {
            boolean[] mask=task.mask(slots,true);
            for(int head=0;head<6;head++)for(int option=0;option<Schema.HEADS[head];option++)
                check(mask[Task.offset(head)+option]==(option==Schema.IDLE[head]),"world controls neutral while a menu is open");
            for(int operation=0;operation<6;operation++)
                check(mask[Task.offset(6)+operation],"all generic menu operations remain available across goal changes");
            for(int slot=0;slot<64;slot++)
                check(mask[Task.offset(7)+slot]==(slot<slots),"only nonexistent slots are removed, never an answer mask");
        }
        boolean[] closed=Task.LOG_TO_WORKBENCH.mask(0,false);
        for(int head=0;head<6;head++)for(int option=0;option<Schema.HEADS[head];option++)
            check(closed[Task.offset(head)+option],"all world controls available after closing");
        Pocket pocket=new Pocket();pocket.setStorage(5,new Stack("OAK_LOG",1));pocket.select(5);
        pocket.click(4,0,Pocket.NONE);pocket.click(1,5,Pocket.NONE);pocket.click(1,39,Pocket.NONE);pocket.click(1,40,Pocket.NONE);
        check(pocket.crafted.getOrDefault("OAK_PLANKS",0L)==4,"literal alternate-slot recipe still required");
        pocket.click(5,0,Pocket.NONE);check(pocket.selected()==5,"closing the menu does not select a hotbar slot");
        System.out.println("PASS menu-focus checks="+checks);
    }
}
