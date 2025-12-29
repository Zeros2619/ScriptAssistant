# ScriptAssistant / uiautomator2自动化脚本助手
## 在PyCharm插件市场搜索ScriptAssistant，安装即可。
## 工具简介
**ScriptAssistant** - A powerful tool designed for Android automation testers and developers to enhance efficiency in writing uiautomator2 scripts.

**uiautomator2自动化脚本助手** - 专为Android自动化测试工程师和开发者设计的高效开发工具，可提升uiautomator2脚本编写效率。

![image](https://github.com/user-attachments/assets/7b012ea5-1f5b-4290-b4f5-8ffc4b304551)
---

## Key Features / 主要功能
| 英文描述 | 中文描述 |
|------------------------------|------------------------------|
| **Automation Script Recording**: Automatically generates uiautomator2 test scripts through simple operations, reducing manual coding efforts. | **自动化脚本录制**：通过简单操作自动生成uiautomator2测试脚本，减少手动编码工作。 |
| **Intelligent Code Completion**: Provides smart suggestions and code completion features for the uiautomator2 API. | **智能代码补全**：提供针对uiautomator2 API的智能提示和代码补全功能。 |


---

## Operation Guide / 操作指引
### Prerequisites / 前置要求
- English: Configure the Python interpreter in PyCharm and install the uiautomator2 library.
- 中文：需要在PyCharm中配置python解释器，并安装uiautomator2库。
- For non-PyCharm versions, you need to specify the path to the Python interpreter with the uiautomator2 library installed in the settings.
- 非PyCharm需要在设置中指定已安装uiautomator2库的python解释器路径


### 1. Top Operation Bar / 顶部操作栏
| 操作步骤 | 英文描述 | 中文描述 |
|----------|------------------------------|------------------------------|
| 1 | Click the "Refresh Devices" icon to refresh the list of currently connected Android devices (requires ADB environment variables). | 点击“刷新设备”图标，刷新当前连接的Android设备列表（需要配置adb环境变量）。 |
| 2 | Click the "Device List" dropdown to select the current device. | 点击“设备列表”下拉框，选择当前设备。 |
| 3 | Connect/Disconnect button (connection may take some time). | 连接和断开连接按钮（连接过程可能有一定耗时）。 |
| 4 | In the connected state, a device alias input box will appear (default: "d") for specifying the device alias, which will be referenced in generated code. | 已连接状态下，会显示设备别名输入框（默认值为"d"），用于设置设备别名，生成代码时将引用此别名。 |
| 5 | "Dump" button: Retrieves current interface screenshots and widget information. | Dump按钮：用于获取当前界面截图和控件信息。 |
| 6 | "Selector" checkbox: When checked, only generates Selector code. | Selector单选框：勾选时，将只生成Selector的代码。 |
| 7 | Settings button | 设置按钮 |


### 2. Left Screenshot Area / 左侧界面截图区域
| 操作步骤 | 英文描述 | 中文描述 |
|----------|------------------------------|------------------------------|
| 1 | View widget properties and hierarchy: Left-click on a widget to view its properties and hierarchy. | 查看控件属性和层级：鼠标左键点击控件，查看控件属性和层级。 |
| 2 | Generate widget positioning code: Right-click on a widget (hold Ctrl to force XPath positioning). | 生成控件定位代码：鼠标右键点击控件（按住Ctrl键，强制使用XPath定位）。 |
| 3 | Generate widget click code: Double-click a widget with the left mouse button (hold Ctrl to force XPath positioning). | 生成控件点击代码：鼠标左键双击控件（按住Ctrl键，强制使用XPath定位）。 |
| 4 | Generate coordinate click code: Hold Ctrl + left-click to generate percentage coordinate click code. | 生成坐标点击代码：按住Ctrl键 + 鼠标左键点击，生成百分比坐标点击代码。 |
| 5 | Generate percentage coordinate swipe code: Hold left mouse button and drag (hold Ctrl to generate drag code). | 生成百分比坐标滑动代码：鼠标左键按住拖动（按住Ctrl键，生成拖拽代码）。 |
| 6 | Generate pixel coordinate swipe code: Hold right mouse button and drag (hold Ctrl to generate drag code). | 生成像素坐标滑动代码：鼠标右键按住拖动（按住Ctrl键，生成拖拽代码）。 |


### 3. Upper Right Widget Tree Area / 右上控件树区域
| 操作步骤 | 英文描述 | 中文描述 |
|----------|------------------------------|------------------------------|
| 1 | Left-click to select a widget; the selected widget range will be marked in the screenshot. | 左键选择对应控件，截图中也将标记选中的控件范围。 |
| 2 | Right-click on a widget to generate its positioning code (useful for overlapping widgets). | 右键点击控件，生成对应控件的定位代码（方便在控件区域重叠时，生成指定控件的定位代码）。 |


### 4. Lower Right Selected Widget Information Area / 右下当前选中控件信息区域
| 操作步骤 | 英文描述 | 中文描述 |
|----------|------------------------------|------------------------------|
| 1 | Left-click on a row of widget properties to automatically copy the property code to the clipboard. | 左键选中一行控件属性时，将自动复制当前属性代码到粘贴板。 |
| 2 | Right-click on a row of widget properties to automatically insert the property into the positioning code at the cursor position (for adding specific positioning parameters). | 右键选中一行控件属性时，将自动将控件属性插入到编辑光标所在行的定位代码中（用于添加指定定位参数）。 |


### 5. Bottom Operation Bar / 底部操作栏
| 操作步骤 | 英文描述 | 中文描述 |
|----------|------------------------------|------------------------------|
| 1 | Displays the positioning code of the selected widget. | 显示已选中控件的定位代码。 |
| 2 | "Find" button: Finds the corresponding widget position on the "real device current interface" through positioning code and displays it on the tool interface. | Find按钮通过定位代码查找“真实设备当前界面”中对应的控件位置，显示在工具界面上。 |

---

## Version History / 版本更新记录

### Version 1.6
- 支持设置指定的Python解释器，以便非PyCharm环境也能正常使用
- 修复已知问题

---

### 插件详情:

* **插件名称**: ScriptAssistant
* **版本**: 1.6
* **描述**: uiautomator2自动化脚本助手：包含自动化脚本录制，智能代码补全等功能，提升自动化脚本编写效率。
* **目标 IDE 平台**: PyCharm (PC) 和 所有基于IntelliJ的IDE平台
* **支持的 IDE 版本**: 从版本 213 到 最新
