#!/usr/bin/env python3
import os
import json
import argparse
from pathlib import Path

# Tree-sitter setup
import tree_sitter
try:
    import tree_sitter_python
    import tree_sitter_javascript
    import tree_sitter_typescript
    import tree_sitter_java
    import tree_sitter_go
    import tree_sitter_ruby
except ImportError:
    print("Warning: Some tree-sitter grammars not installed.")

# Language mapping
LANG_MAP = {
    ".py": ("python", tree_sitter_python),
    ".js": ("javascript", tree_sitter_javascript),
    ".jsx": ("javascript", tree_sitter_javascript),
    ".ts": ("typescript", tree_sitter_typescript),
    ".tsx": ("tsx", tree_sitter_typescript),
    ".java": ("java", tree_sitter_java),
    ".go": ("go", tree_sitter_go),
    ".rb": ("ruby", tree_sitter_ruby)
}

def get_parser(ext):
    if ext not in LANG_MAP:
        return None, None
    
    lang_name, module = LANG_MAP[ext]
    try:
        # tree-sitter v0.22.0+ API
        language = tree_sitter.Language(module.language())
        parser = tree_sitter.Parser(language)
        return lang_name, parser
    except AttributeError:
        # Older tree-sitter API fallback
        try:
             language = tree_sitter.Language(module.language(), lang_name)
             parser = tree_sitter.Parser()
             parser.set_language(language)
             return lang_name, parser
        except Exception as e:
             print(f"Failed to load parser for {ext}: {e}")
             return None, None
    except Exception as e:
        print(f"Failed to load parser for {ext}: {e}")
        return None, None

def extract_python_info(node, source_bytes):
    funcs = []
    classes = []
    imports = []
    
    def traverse(n):
        if n.type == 'function_definition':
            name_node = n.child_by_field_name('name')
            if name_node:
                funcs.append({
                    "name": source_bytes[name_node.start_byte:name_node.end_byte].decode('utf8', 'ignore'),
                    "line_start": n.start_point[0] + 1,
                    "line_end": n.end_point[0] + 1
                })
        elif n.type == 'class_definition':
            name_node = n.child_by_field_name('name')
            if name_node:
                classes.append({
                    "name": source_bytes[name_node.start_byte:name_node.end_byte].decode('utf8', 'ignore'),
                    "line_start": n.start_point[0] + 1,
                    "line_end": n.end_point[0] + 1
                })
        elif n.type in ('import_statement', 'import_from_statement'):
            imports.append(source_bytes[n.start_byte:n.end_byte].decode('utf8', 'ignore'))
        
        for child in n.children:
            traverse(child)
            
    traverse(node)
    return funcs, classes, imports

def extract_js_ts_info(node, source_bytes):
    funcs = []
    classes = []
    imports = []
    
    def traverse(n):
        if n.type in ('function_declaration', 'method_definition', 'arrow_function'):
            name_node = n.child_by_field_name('name')
            if name_node:
                funcs.append({
                    "name": source_bytes[name_node.start_byte:name_node.end_byte].decode('utf8', 'ignore'),
                    "line_start": n.start_point[0] + 1,
                    "line_end": n.end_point[0] + 1
                })
        elif n.type == 'class_declaration':
            name_node = n.child_by_field_name('name')
            if name_node:
                classes.append({
                    "name": source_bytes[name_node.start_byte:name_node.end_byte].decode('utf8', 'ignore'),
                    "line_start": n.start_point[0] + 1,
                    "line_end": n.end_point[0] + 1
                })
        elif n.type == 'import_statement':
            imports.append(source_bytes[n.start_byte:n.end_byte].decode('utf8', 'ignore'))
            
        for child in n.children:
            traverse(child)
            
    traverse(node)
    return funcs, classes, imports

def extract_java_info(node, source_bytes):
    funcs = []
    classes = []
    imports = []
    
    def traverse(n):
        if n.type == 'method_declaration':
            name_node = n.child_by_field_name('name')
            if name_node:
                funcs.append({
                    "name": source_bytes[name_node.start_byte:name_node.end_byte].decode('utf8', 'ignore'),
                    "line_start": n.start_point[0] + 1,
                    "line_end": n.end_point[0] + 1
                })
        elif n.type == 'class_declaration':
            name_node = n.child_by_field_name('name')
            if name_node:
                classes.append({
                    "name": source_bytes[name_node.start_byte:name_node.end_byte].decode('utf8', 'ignore'),
                    "line_start": n.start_point[0] + 1,
                    "line_end": n.end_point[0] + 1
                })
        elif n.type == 'import_declaration':
            imports.append(source_bytes[n.start_byte:n.end_byte].decode('utf8', 'ignore'))
            
        for child in n.children:
            traverse(child)
            
    traverse(node)
    return funcs, classes, imports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True, help="Output JSON file path")
    args = parser.parse_args()

    results = {}
    
    for root, _, files in os.walk('.'):
        if '.git' in root:
            continue
            
        for file in files:
            path = Path(root) / file
            ext = path.suffix
            
            lang_name, ts_parser = get_parser(ext)
            if not ts_parser:
                continue
                
            try:
                with open(path, 'rb') as f:
                    source_bytes = f.read()
            except Exception as e:
                print(f"Skipping {path}: {e}")
                continue
                
            tree = ts_parser.parse(source_bytes)
            
            funcs, classes, imports = [], [], []
            if lang_name == "python":
                funcs, classes, imports = extract_python_info(tree.root_node, source_bytes)
            elif lang_name in ("javascript", "typescript", "tsx"):
                funcs, classes, imports = extract_js_ts_info(tree.root_node, source_bytes)
            elif lang_name == "java":
                funcs, classes, imports = extract_java_info(tree.root_node, source_bytes)
            # Add other language extractors as needed
            
            # Use relative path as key (e.g. src/main.py)
            rel_path = str(path.relative_to('.')).replace('\\', '/')
            results[rel_path] = {
                "language": lang_name,
                "ast": {
                    "functions": funcs,
                    "classes": classes,
                    "imports": imports
                },
                "metrics": {
                    "lines_of_code": len(source_bytes.splitlines()),
                    "function_count": len(funcs),
                    "class_count": len(classes),
                    "import_count": len(imports)
                }
            }

    with open(args.output, 'w') as f:
        json.dump(results, f, indent=2)
    print(f"Extracted AST for {len(results)} files to {args.output}")

if __name__ == "__main__":
    main()
