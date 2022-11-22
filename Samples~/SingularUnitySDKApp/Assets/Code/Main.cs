using System;
using System.Collections;
using System.Collections.Generic;
using System.Net;
using System.Reflection;
using System.Runtime.Serialization;
using UnityEngine;
using UnityEngine.Purchasing;
using UnityEngine.UI;
using Singular.SDK;


public class Main :  MonoBehaviour, SingularDeferredDeepLinkHandler, SingularLinkHandler, SingularConversionValueUpdatedHandler, IStoreListener{

	private int m_Updates = 0;
	private float m_Speed = 0.5f;

	private Vector3 m_RotationDirection = Vector3.up;

	private IStoreController controller;
    private IExtensionProvider extensions;


	public void OnInitialized (IStoreController controller, IExtensionProvider extensions)
    {
        this.controller = controller;
        this.extensions = extensions;
    }

	public void OnInitializeFailed (InitializationFailureReason error)
    {
    }

	


    public PurchaseProcessingResult ProcessPurchase (PurchaseEventArgs e)
    {
		#if UNITY_5_3_OR_NEWER
		SingularSDK.InAppPurchase( e.purchasedProduct, new Dictionary<string, object>() { ["my_atr"] = "attribute" },false);
		#endif
        return PurchaseProcessingResult.Complete;
    }


    public void OnPurchaseFailed (Product i, PurchaseFailureReason p)
    {
    }

	void Start () {
		var builder = ConfigurationBuilder.Instance(StandardPurchasingModule.Instance());
        builder.AddProduct("prod_1", ProductType.Consumable, new IDs
        {
            {"prod_1", GooglePlay.Name},
            {"prod_1", MacAppStore.Name}
        });

        UnityPurchasing.Initialize (this, builder);
	}

	void ToggleRotationDirection()
	{
		Debug.Log ("ToggleRotationDirection Called");

		if (m_RotationDirection == Vector3.up)
		{
			m_RotationDirection = Vector3.down;
		}
		else 
		{
			m_RotationDirection = Vector3.up;
		}
	}

	void Update()
	{
		transform.Rotate(m_RotationDirection * m_Speed + Vector3.back * m_Speed);

		if (m_Updates % 200 == 0) {
			ToggleRotationDirection ();
		}

		m_Updates++;
	}

	// app button implementations

	public void InitSDK_button_OnClick() {
        SingularSDK.SetCustomUserId("mytestcustomuserid");
		SingularSDK.RegisterTokenForUninstall("4D23F1D94C3383FEF6063F791CC14F4F8B3F51395DA5B87A4644C2F50DF38CFF");
		SingularSDK.SetConversionValueUpdatedHandler(this);
		SingularSDK.InitializeSingularSDK();
	}

	public void SendSimpleEvent_button_OnClick() { 
        SingularSDK.Event("SimpleUnityEvent!");
        SingularSDK.UnsetCustomUserId();
    }

	public void SendJSONEvent_button_OnClick() {
		SingularSDK.Event(new Dictionary<string, object>() {
			{"property_a", "aaaa"},
			{"property_b", new Dictionary<string, object>() {
					{"sub_property_a", "ssssa"},
					{"sub_property_b", "ssssb"}
				}
			}
		}, "UnityJSONEvent!");
    }

	public void ShortLink_button_OnClick() {

		void callback(string data, string error){
			Debug.Log ("link: "+ data + " error: " + error);		
		}
		SingularSDK.createReferrerShortLink("https://sample.sng.link/B4tbm/v8fp?_dl=https%3A%2F%2Fabc.com", "refName", "refID", 
		  new Dictionary<string, string>() {
			{"channel", "sms"}
		}, callback);
    }

	public void SendRevenue_button_OnClick() {
        SingularSDK.Revenue("USD", 77.7);
        SingularSDK.SetCustomUserId("UnityCustomUserID123456");
    }

	public void IAP_button_OnClick() {
    	controller.InitiatePurchase("prod_1");
	}

	public void RegisterDdlHandler_button_OnClick() {
		SingularSDK.SetDeferredDeepLinkHandler (this);
        //SingularSDK.SetSingularLinkHandler(this);
    }

    public void OnDeferredDeepLink(string deepLink){
		Debug.Log ("final deeplink received in delegate methods---->" + deepLink);
        GameObject.Find("SendRevenueEvent_button").GetComponentInChildren<Text>().text =
            string.Format("{0}", deepLink);
    }

    public void OnSingularLinkResolved(SingularLinkParams linkParams) {

        Debug.Log("final deeplink received in delegate methods---->" + linkParams.Deeplink);
        Debug.Log("final passthrough received in delegate methods---->" + linkParams.Passthrough);
        Debug.Log("final is_deferred received in delegate methods---->" + linkParams.IsDeferred);

        GameObject.Find("SendRevenueEvent_button").GetComponentInChildren<Text>().text = 
            string.Format("{0},{1},{2}", linkParams.Deeplink, linkParams.Passthrough, linkParams.IsDeferred);
    }

    private void SendInAppPurchase() {
        var product = BuildProduct();
		#if UNITY_5_3_OR_NEWER
        SingularSDK.InAppPurchase("my_purchase", product, new Dictionary<string, object>() { ["my_atr"] = "attribute" });
		#endif
	}

    private static Product BuildProduct() {
        // We can't simply create product with a constructor. So I used reflection to create one.
        PayoutDefinition payout = new PayoutDefinition("subtype", 3);
        ProductDefinition definition = new ProductDefinition("product_id", "store_id", ProductType.Consumable, true, payout);
        ProductMetadata metadata = new ProductMetadata("15.00", "my_product", "my_description", "USD",decimal.Parse("15.00"));

        var ctor = typeof(Product).GetConstructor(BindingFlags.NonPublic | BindingFlags.Instance, null, new[] { typeof(ProductDefinition), typeof(ProductMetadata), typeof(string) }, null);
        Product product = ctor.Invoke(new object[] { definition, metadata, AndroidProductReceipt }) as Product;

        return product;
    }

    public void OnConversionValueUpdated(int value) {
		GameObject.Find("SendRevenueEvent_button").GetComponentInChildren<Text>().text =
			string.Format("value = {0}", value);
	}

    private static string AndroidProductReceipt = "{\"signature\":\"com.sample.goo\",\"orderId\":\"transactionId.android.test.purchased\",\"productId\":\"android.test.purchased\",\"developerPayload\":\"PRODUCT_SKU_AND_USER_ID_AND_DATE\",\"purchaseTime\":0,\"purchaseState\":0,\"purchaseToken\":\"inapp:com.sample.goo:android.test.purchased\"}";
    private static string IOSProductReceipt = "{\"Store\":\"AppleAppStore\",\"TransactionID\":\"1000000537718845\",\"Payload\":\"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\"}";
}