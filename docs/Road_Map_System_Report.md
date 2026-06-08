# 4. Road & Map System

## 4.1. Mục tiêu

Road & Map System là module cốt lõi chịu trách nhiệm xây dựng và quản lý toàn bộ cơ sở hạ tầng giao thông tĩnh trong hệ thống Smart City Traffic Simulation. Module này cung cấp không gian di chuyển cho các phương tiện, quản lý mạng lưới đường bộ, các nút giao thông (ngã ba, ngã tư, ngã năm) và cơ chế sinh phương tiện (spawning) tại các điểm đầu vào của bản đồ.

Hệ thống được thiết kế linh hoạt, cho phép mở rộng quy mô từ một ngã rẽ đơn lẻ thành một mạng lưới giao thông phức tạp. Đặc biệt, hệ thống hỗ trợ cơ chế thay đổi tỷ lệ hiển thị (scale) động, giúp các đối tượng tại khu vực ngã rẽ được phóng to để quan sát rõ ràng chi tiết tương tác, trong khi tại các tuyến đường thẳng rộng lớn, hình ảnh sẽ được thu nhỏ để mang lại cái nhìn bao quát toàn cảnh.

## 4.2. Kiến trúc và Các thành phần chính

Kiến trúc của Road & Map System được tổ chức thành các lớp đối tượng chuyên biệt, mỗi lớp đảm nhận một vai trò cụ thể trong mạng lưới giao thông:

### MapManager
`MapManager` đóng vai trò là trung tâm điều khiển toàn bộ bản đồ. Lớp này quản lý danh sách tất cả các con đường (`Road`), ngã rẽ (`Junction`) và các điểm sinh phương tiện (`VehicleSpawner`). Nhờ áp dụng mẫu thiết kế Singleton, hệ thống đảm bảo chỉ tồn tại duy nhất một phiên bản của bản đồ trong suốt quá trình mô phỏng, giúp đồng bộ hóa dữ liệu và dễ dàng truy cập từ các module khác.

### Road và Lane
Mỗi con đường (`Road`) được cấu tạo từ nhiều làn đường (`Lane`).
- Lớp `Road` lưu trữ thông tin tổng thể như chiều dài, chiều rộng, vị trí bắt đầu, vị trí kết thúc và quản lý danh sách các làn đường thuộc về nó.
- Lớp `Lane` đại diện cho một làn di chuyển cụ thể với hướng di chuyển cố định (Bắc, Nam, Đông, Tây) và giới hạn tốc độ riêng. Mỗi làn đường duy trì một danh sách các phương tiện đang lưu thông trên đó, hỗ trợ việc tính toán mức độ tắc nghẽn và khoảng cách an toàn.

### Junction (Ngã rẽ)
Lớp `Junction` quản lý các nút giao thông phức tạp, nơi các con đường giao nhau. Hệ thống hỗ trợ đa dạng các loại ngã rẽ thông qua Enum `JunctionType` bao gồm ngã ba (THREE_WAY), ngã tư (FOUR_WAY) và ngã năm (FIVE_WAY). Mỗi ngã rẽ quản lý danh sách các con đường kết nối tới nó và có thể tích hợp với hệ thống đèn tín hiệu giao thông (Traffic Light System) để điều tiết luồng xe.

### VehicleSpawner
Cơ chế sinh phương tiện được xử lý thông qua lớp `VehicleSpawner`. Các đối tượng này được đặt tại các điểm đầu vào của mạng lưới (ví dụ: đầu các làn đường). Dựa trên thông số tỷ lệ sinh (`spawnRate`) được cấu hình, spawner sẽ tự động tạo ra các phương tiện ngẫu nhiên hoặc theo tỷ lệ nhất định (như ô tô, xe máy, xe ưu tiên) và đưa chúng vào luồng giao thông.

### Location
Lớp `Location` quản lý hệ tọa độ (x, y) và tỷ lệ hiển thị (`scale`) của các đối tượng. Lớp này cung cấp các phương thức tính toán khoảng cách và hỗ trợ cơ chế phóng to/thu nhỏ động theo yêu cầu của đề tài.

## 4.3. Các kỹ thuật lập trình hướng đối tượng được áp dụng

Trong quá trình phát triển Road & Map System, nhóm đã áp dụng triệt để các nguyên lý lập trình hướng đối tượng (OOP) nhằm đảm bảo mã nguồn rõ ràng, dễ bảo trì và có khả năng mở rộng cao.

### 4.3.1. Encapsulation (Đóng gói)
Kỹ thuật đóng gói được thể hiện rõ nét qua việc che giấu các thuộc tính nội bộ của các lớp như `Road`, `Lane`, `Junction` bằng từ khóa `private`. Việc truy cập và sửa đổi các thuộc tính này (như danh sách phương tiện trên một làn, mức độ tắc nghẽn, tỷ lệ sinh xe) chỉ được thực hiện thông qua các phương thức getter và setter công khai. Điều này giúp bảo vệ tính toàn vẹn của dữ liệu và ngăn chặn những can thiệp không hợp lệ từ bên ngoài hệ thống.

### 4.3.2. Composition (Thành phần)
Thay vì sử dụng kế thừa một cách lạm dụng, hệ thống sử dụng quan hệ Composition (has-a) để xây dựng cấu trúc bản đồ. Một `MapManager` chứa nhiều `Road` và `Junction`. Một `Road` lại chứa nhiều `Lane`. Cấu trúc phân cấp này phản ánh chính xác mối quan hệ thực tế giữa các thực thể giao thông, đồng thời giúp việc quản lý vòng đời đối tượng trở nên tự nhiên và logic hơn.

### 4.3.3. Singleton Pattern
Mẫu thiết kế Singleton được áp dụng cho lớp `MapManager`. Trong một hệ thống mô phỏng giao thông, việc có nhiều phiên bản bản đồ hoạt động song song có thể dẫn đến xung đột dữ liệu (ví dụ: một phương tiện xuất hiện trên hai bản đồ khác nhau). Singleton đảm bảo rằng mọi module (như Vehicle System, GUI) đều tương tác với cùng một cơ sở dữ liệu bản đồ duy nhất thông qua phương thức `MapManager.getInstance()`.

### 4.3.4. Tính mở rộng (Extensibility)
Hệ thống được thiết kế để dễ dàng mở rộng. Khi cần thêm một loại ngã rẽ mới (ví dụ: vòng xuyến), lập trình viên chỉ cần bổ sung loại mới vào Enum `JunctionType` và cập nhật logic kiểm tra hợp lệ mà không làm phá vỡ cấu trúc hiện tại. Tương tự, việc thêm các loại phương tiện mới vào `VehicleSpawner` được thực hiện dễ dàng thông qua phương thức `addVehicleType()`, hoàn toàn độc lập với phần logic xử lý chính của bản đồ.

## 4.4. Cơ chế Zoom In / Zoom Out

Một trong những yêu cầu đặc thù của đề tài là khả năng thay đổi kích thước hiển thị tùy thuộc vào vị trí của phương tiện. Road & Map System hỗ trợ tính năng này thông qua thuộc tính `scale` được tích hợp sẵn.

- **Tại ngã rẽ:** Các đối tượng `Junction` được khởi tạo với hệ số `scale > 1.0` (ví dụ: 1.5). Khi phương tiện tiến vào khu vực quản lý của ngã rẽ, hệ thống GUI có thể truy xuất hệ số này thông qua `junction.getScale()` để phóng to hình ảnh phương tiện và đèn giao thông, giúp người dùng quan sát rõ các hành vi tương tác phức tạp như nhường đường, vượt, dừng đèn đỏ.
- **Tại đường thẳng:** Các đối tượng `Road` duy trì hệ số `scale = 1.0` (hoặc nhỏ hơn đối với các mạng lưới rộng lớn). Khi phương tiện rời khỏi ngã rẽ và đi vào đường thẳng, hình ảnh sẽ được thu nhỏ lại tương ứng, cho phép hiển thị một vùng không gian rộng lớn hơn với nhiều phương tiện đang lưu thông cùng lúc.

## 4.5. Kết quả đạt được

Module Road & Map System đã hoàn thành xuất sắc vai trò nền tảng không gian cho hệ thống mô phỏng. Nó không chỉ đáp ứng đầy đủ các yêu cầu về việc tạo lập ngã ba, ngã tư, ngã năm và mạng lưới đường rộng lớn, mà còn cung cấp cơ chế sinh phương tiện tự động và linh hoạt. Kiến trúc hướng đối tượng vững chắc của module này tạo điều kiện thuận lợi tối đa cho việc tích hợp với các module khác như Vehicle System, Driver AI và GUI trong giai đoạn hoàn thiện dự án.
