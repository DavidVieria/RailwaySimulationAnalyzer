package pt.ipp.isep.dei.ui.console.menu;

import pt.ipp.isep.dei.ui.console.*;
import pt.ipp.isep.dei.ui.console.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AdminUI implements Runnable {

    public AdminUI() {}

    public void run() {
        List<MenuItem> options = new ArrayList<MenuItem>();
        options.add(new MenuItem("Create Task", new CreateTaskUI()));
        options.add(new MenuItem("Railway Analysis & Maintenance Tools (MDISC)", new MDISCUI()));

        int option = 0;
        do {
            option = Utils.showAndSelectIndex(options, "\n\n===== ADMIN MENU =====\n");

            if ((option >= 0) && (option < options.size())) {
                options.get(option).run();
            }
        } while (option != -1);
    }
}
