package common;

import java.util.Map;

public class TestJSON {
  public static void main(String[] args) {
    String json = "{\"type\":\"REGISTER_DRIVER_PENDING\",\"payload\":{\"full_name\":\"Test Driver\",\"dob\":\"1990-01-01\",\"gender\":\"Male\",\"nationality\":\"Ethiopian\",\"id_number\":\"ET12345\",\"phone\":\"+251911223344\",\"email\":\"test@driver.com\",\"address\":\"Addis Ababa\",\"license_number\":\"LIC123\",\"license_type\":\"Auto\",\"license_issue_date\":\"2020-01-01\",\"license_expiry_date\":\"2030-01-01\",\"vehicle_type\":\"Car\",\"vehicle_model\":\"Toyota\",\"vehicle_year\":\"2015\",\"license_plate\":\"AA-12345\",\"username\":\"antigravity_test_1\",\"password\":\"123\"}}";

    System.out.println("Testing JSON parsing...");
    Message msg = JSONUtil.fromJSON(json);

    if (msg == null) {
      System.out.println("FAILED: Message is null");
    } else {
      System.out.println("SUCCESS: Type = " + msg.getType());
      Map<String, Object> payload = msg.getPayload();
      System.out.println("Payload size = " + payload.size());
      for (String key : payload.keySet()) {
        System.out.println("  " + key + " = " + payload.get(key));
      }
    }
  }
}
