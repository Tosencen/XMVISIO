#!/usr/bin/env python3
"""
Fix ELF shared library alignment for 16KB page size (Android 15+).
- Sets p_align of LOAD segments to 0x4000 (16384)
- Adjusts p_vaddr and p_offset to satisfy congruence: p_offset % p_align == p_vaddr % p_align
- Removes corrupted .so files (not valid ELF)

Usage: python3 fix_elf_alignment.py <native-libs-dir>
"""

import os
import struct
import sys

NEW_ALIGN = 0x4000  # 16KB


def is_valid_elf(filepath):
    with open(filepath, 'rb') as f:
        magic = f.read(4)
    return magic == b'\x7fELF'


def read_elf_info(data):
    if data[:4] != b'\x7fELF':
        return None
    ei_class = data[4]
    if ei_class != 2:  # 64-bit only
        return None
    
    e_phoff = struct.unpack_from('<Q', data, 32)[0]
    e_phentsize = struct.unpack_from('<H', data, 54)[0]
    e_phnum = struct.unpack_from('<H', data, 56)[0]
    e_shoff = struct.unpack_from('<Q', data, 40)[0]
    e_shentsize = struct.unpack_from('<H', data, 58)[0]
    e_shnum = struct.unpack_from('<H', data, 60)[0]
    
    return {
        'ei_class': ei_class,
        'e_phoff': e_phoff,
        'e_phentsize': e_phentsize,
        'e_phnum': e_phnum,
        'e_shoff': e_shoff,
        'e_shentsize': e_shentsize,
        'e_shnum': e_shnum,
    }


def patch_elf_16k(filepath):
    """Patch a single ELF file for 16KB alignment."""
    with open(filepath, 'rb') as f:
        data = bytearray(f.read())
    
    info = read_elf_info(data)
    if info is None:
        return False
    
    e_phoff = info['e_phoff']
    e_phentsize = info['e_phentsize']
    e_phnum = info['e_phnum']
    
    # Read all program headers
    segments = []
    for i in range(e_phnum):
        offset = e_phoff + i * e_phentsize
        seg_data = data[offset:offset + e_phentsize]
        p_type = struct.unpack_from('<I', seg_data, 0)[0]
        segments.append({
            'index': i,
            'type': p_type,
            'flags': struct.unpack_from('<I', seg_data, 4)[0],
            'offset': struct.unpack_from('<Q', seg_data, 8)[0],
            'vaddr': struct.unpack_from('<Q', seg_data, 16)[0],
            'filesz': struct.unpack_from('<Q', seg_data, 32)[0],
            'memsz': struct.unpack_from('<Q', seg_data, 40)[0],
            'align': struct.unpack_from('<Q', seg_data, 48)[0],
        })
    
    load_segs = [s for s in segments if s['type'] == 1]
    
    if not load_segs:
        return True  # No LOAD segments, nothing to fix
    
    # Check if already fixed
    if all(s['align'] >= NEW_ALIGN for s in load_segs):
        all_congruent = True
        for s in load_segs:
            if (s['offset'] % NEW_ALIGN) != (s['vaddr'] % NEW_ALIGN):
                all_congruent = False
                break
        if all_congruent:
            return True
    
    print(f"  Patching {os.path.basename(filepath)} ({len(load_segs)} LOAD segs)")
    
    # Patch each LOAD segment
    for idx, seg in enumerate(load_segs):
        seg_idx = seg['index']
        seg_offset = e_phoff + seg_idx * e_phentsize
        
        if idx == 0:
            # First segment: just update p_align
            struct.pack_into('<Q', data, seg_offset + 48, NEW_ALIGN)
        else:
            prev_seg = load_segs[idx - 1]
            prev_end_vaddr = prev_seg['vaddr'] + prev_seg['memsz']
            
            vaddr_mod = seg['vaddr'] % NEW_ALIGN
            offset_mod = seg['offset'] % NEW_ALIGN
            
            # Need to adjust: find new p_vaddr >= prev_end_vaddr
            # such that p_vaddr % NEW_ALIGN == offset_mod
            base_vaddr = max(seg['vaddr'], prev_end_vaddr)
            remainder = base_vaddr % NEW_ALIGN
            if remainder <= offset_mod:
                new_vaddr = base_vaddr + (offset_mod - remainder)
            else:
                new_vaddr = base_vaddr + (NEW_ALIGN - remainder + offset_mod)
            
            # Keep p_offset as-is, adjust p_vaddr
            struct.pack_into('<Q', data, seg_offset + 16, new_vaddr)  # p_vaddr
            struct.pack_into('<Q', data, seg_offset + 48, NEW_ALIGN)  # p_align
    
    # Verify
    all_ok = True
    for i in range(e_phnum):
        seg_data = data[e_phoff + i * e_phentsize:e_phoff + (i + 1) * e_phentsize]
        p_type = struct.unpack_from('<I', seg_data, 0)[0]
        if p_type == 1:
            p_align = struct.unpack_from('<Q', seg_data, 48)[0]
            p_offset = struct.unpack_from('<Q', seg_data, 8)[0]
            p_vaddr = struct.unpack_from('<Q', seg_data, 16)[0]
            congruent = (p_offset % p_align) == (p_vaddr % p_align)
            ok = p_align >= NEW_ALIGN and congruent
            if not ok:
                all_ok = False
                print(f"    FAIL: LOAD[{i}]: offset=0x{p_offset:x} vaddr=0x{p_vaddr:x} align=0x{p_align:x} congruent={congruent}")
    
    if all_ok:
        with open(filepath, 'wb') as f:
            f.write(data)
        print(f"  OK")
    else:
        print(f"  FAILED! Not writing changes.")
    
    return all_ok


def process_directory(root_dir):
    """Recursively process all .so files in directory tree."""
    fixed = 0
    removed = 0
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for fname in filenames:
            if not fname.endswith('.so'):
                continue
            fpath = os.path.join(dirpath, fname)
            
            if not is_valid_elf(fpath):
                if fname == 'libc++_shared.so':
                    # Corrupted file, remove it
                    os.remove(fpath)
                    removed += 1
                    print(f"  Removed corrupted {fpath}")
                continue
            
            if patch_elf_16k(fpath):
                fixed += 1
    
    return fixed, removed


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: fix_elf_alignment.py <native-libs-dir>")
        sys.exit(1)
    
    root = sys.argv[1]
    if not os.path.isdir(root):
        print(f"Directory not found: {root}")
        sys.exit(1)
    
    print(f"Processing native libs in: {root}")
    fixed, removed = process_directory(root)
    print(f"\nDone: {fixed} files patched, {removed} corrupted files removed")
