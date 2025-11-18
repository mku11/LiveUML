package com.mku.liveuml.model.entities;

import java.util.List;

public class Package {

    public static String getFullName(String packageName, String name, List<String> parents) {
        String fullName = "";
        String parentsName = parents == null ? "" : String.join("$", parents);
        if (packageName != null && packageName.length() > 0)
            fullName = packageName + ".";
        if (parentsName.length() > 0)
            fullName += parentsName + "$" + name;
        else
            fullName += name;
        return fullName;
    }
}
