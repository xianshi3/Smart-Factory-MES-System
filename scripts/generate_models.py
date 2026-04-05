print('生成轻量级ONNX模型...')

import os
import struct

def create_minimal_onnx(file_path):
    with open(file_path, 'wb') as f:
        f.write(b'\x08\x07\x12\x0bsimple_model\x1a\x0csimple_model"\x00')
        f.write(b'\x00' * 100)

os.makedirs('mes-ai-service/src/models/saved_models', exist_ok=True)

create_minimal_onnx('mes-ai-service/src/models/saved_models/quality_predict.onnx')
create_minimal_onnx('mes-ai-service/src/models/saved_models/output_predict.onnx')

print('已创建示例ONNX模型文件')

import os
for f in os.listdir('mes-ai-service/src/models/saved_models'):
    size = os.path.getsize(f'mes-ai-service/src/models/saved_models/{f}')
    print(f'  {f}: {size} bytes')