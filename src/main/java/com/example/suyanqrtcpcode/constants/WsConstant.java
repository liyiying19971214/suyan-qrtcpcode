package com.example.suyanqrtcpcode.constants;

public interface WsConstant {
    interface constant{
        public  static  final String  PRINTER_CONFIG_BUCKETMODE="1";
    }
	interface msgtype{
		public static final String CELLPHONE_CONNECTION_STATUS="cellphone_status";
		public static final String PRINTER_CONNECTION_STATUS="printer_status";
		public static final String DATA_TRANSFER_FLAG="data_transfer";
		public static final String PRINTER_READY_FLAG="printer_ready";
		public static final String CELLPHONE_CONNECTED_IP="cellphone_ip";
		public static final String PRINTER_CONNECTED_IP="printer_ip";
		public static final String CELLPHONE_SEND_MSG="cellphone_send_msg";
		public static final String PRINTER_RECEIVE_MSG="printer_receive_msg";
	   public static final String SERVER_SEND_MSG="server_send_msg";

	   public  static  final  String  PRINTER_UPLOAD_STATUS="printer_upload_status";
	

	   //创建水桶信息
       public  static final String GENERATE_BUCKET_FLAG="generate_bucket";

       public  static final String GENERATE_BUCKET_MSG="generate_bucket_msg";


		//报废水桶信息
		public  static final String DESTRUCTION_ALERT_MSG="destruction_alert_msg";

		public static final String HEART_RECEIVE_MSG="heart_data";
		public static final String ERROR_MSG_DATA="error_msg";
		public static final String PRINT_CNT="print_cnt";


		public static final String SET_SCRAP_BUCKET="set_scrap_bucket";


	}
	
	

	
}
