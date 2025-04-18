from flask import Flask, request, jsonify, send_file
from werkzeug.utils import secure_filename
import os
from datetime import datetime

app = Flask(__name__)

# 配置上传文件存储路径
UPLOAD_FOLDER = 'uploads'
AVATAR_FOLDER = os.path.join(UPLOAD_FOLDER, 'avatar')
# 确保目录存在
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)
if not os.path.exists(AVATAR_FOLDER):
    os.makedirs(AVATAR_FOLDER)

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['AVATAR_FOLDER'] = AVATAR_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 限制文件大小为16MB

# 允许的文件类型
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({'error': '没有文件部分'}), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': '没有选择文件'}), 400
    
    if file and allowed_file(file.filename):
        # 生成安全的文件名
        filename = secure_filename(file.filename)
        
        # 检查是否是头像上传
        folder_type = request.form.get('type', '')
        if folder_type == 'avatar':
            # 头像文件直接使用传入的文件名（不添加时间戳）
            # 构建文件路径
            file_path = os.path.join(app.config['AVATAR_FOLDER'], filename)
            
            # 检查是否存在同名文件，存在则先删除
            if os.path.exists(file_path):
                os.remove(file_path)
                
            # 保存新文件
            file.save(file_path)
            
            # 为了客户端兼容性，返回路径包含子文件夹
            filename = 'avatar/' + filename
        else:
            # 非头像文件添加时间戳避免文件名冲突
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S_')
            timestamped_filename = timestamp + filename
            
            # 保存到默认上传文件夹
            file_path = os.path.join(app.config['UPLOAD_FOLDER'], timestamped_filename)
            file.save(file_path)
            
            # 更新返回的文件名
            filename = timestamped_filename
        
        return jsonify({
            'message': '文件上传成功',
            'filename': filename,
            'path': file_path
        }), 200
    
    return jsonify({'error': '不允许的文件类型'}), 400

@app.route('/image/<path:filename>', methods=['GET'])
def get_image(filename):
    try:
        # 路径可能包含子文件夹
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        return send_file(file_path)
    except Exception as e:
        return jsonify({'error': '文件不存在'}), 404

@app.route('/delete', methods=['POST'])
def delete_file():
    filename = request.json.get('filename')

    if not filename:
        return jsonify({'error': '未提供文件名'}), 400

    # 安全检查：确保删除的是上传目录中的文件
    if '..' in filename or filename.startswith('/'):
        return jsonify({'error': '不允许的文件路径'}), 400

    # 构建完整路径
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)

    try:
        if os.path.exists(file_path):
            os.remove(file_path)
            return jsonify({'message': '文件删除成功'}), 200
        else:
            return jsonify({'message': '文件不存在'}), 404
    except Exception as e:
        return jsonify({'error': f'删除失败: {str(e)}'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=9000, debug=True) 