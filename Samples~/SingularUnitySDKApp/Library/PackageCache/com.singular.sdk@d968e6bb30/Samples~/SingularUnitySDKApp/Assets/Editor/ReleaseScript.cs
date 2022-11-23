using System;
using UnityEditor;
using UnityEngine;
using System.IO;
using System.Collections;
using System.Collections.Generic;
using System.Linq;

public class ReleaseScript {

    [MenuItem("Assets/Build Package")]
    public static void BuildPackage() {
        AssetDatabase.ExportPackage(GetAssets().ToArray(), "temp-singular-sdk.unitypackage");
    }

    private static List<string> GetAssets() {
        List<string> assests = new List<string>();

        string[] a = AssetDatabase.GetAllAssetPaths();
        Array.Sort(a);
        assests = a.ToList().Where(x => (x.StartsWith("Assets/Plugins/") || x.StartsWith("Assets/Code/")) &&
        !(x.EndsWith("Main.cs") || x.EndsWith("ReleaseScript.cs"))).ToList();

        return assests;
    }

}

