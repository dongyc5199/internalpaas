import re

def check_viewport_line():
    """检查viewport meta标签的具体问题"""
    
    filepath = r'E:\work\code\internalpaas\src\main\resources\templates\applications.html'
    
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    line_5 = lines[4]  # 第5行，索引从0开始
    
    print(f"第5行内容: {repr(line_5)}")
    print(f"第5行字节: {line_5.encode('utf-8')}")
    
    # 查找content属性
    content_match = re.search(r'content="([^"]*)"', line_5)
    if content_match:
        content_value = content_match.group(1)
        print(f"content属性值: {repr(content_value)}")
    else:
        print("未找到标准的content属性")
        
    # 查找所有属性
    attr_pattern = r'(\w+)=([^>\s]+)'
    matches = re.findall(attr_pattern, line_5)
    
    print("找到的属性:")
    for attr_name, attr_value in matches:
        print(f"  {attr_name} = {repr(attr_value)}")
    
    # 检查是否有多余的引号
    if '""' in line_5:
        print("发现双引号问题!")
        
    return line_5

if __name__ == "__main__":
    check_viewport_line()