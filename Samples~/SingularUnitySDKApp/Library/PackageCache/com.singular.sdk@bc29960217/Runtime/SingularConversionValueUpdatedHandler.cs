using System;
using Newtonsoft.Json;


namespace Singular.SDK{
public interface SingularConversionValueUpdatedHandler {
    void OnConversionValueUpdated(int value);
}

}
