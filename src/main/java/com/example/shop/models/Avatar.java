package com.example.shop.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "avatars")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Avatar {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "originalFileName")
    private String originalFileName;

    @Column(name = "size")
    private Long size;

    @Column(name = "contentType")
    private String contentType;

    @Override
    public String toString() {
        return "Avatar{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", size=" + size +
                ", contentType='" + contentType + '\'' +
                ", bytes=" + Arrays.toString(bytes) +
                '}';
    }


    @Column(name = "bytes", columnDefinition = "LONGBLOB")
    private byte[] bytes;

    @OneToOne(mappedBy = "avatar", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private User user;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avatar avatar = (Avatar) o;
        return Objects.equals(getId(), avatar.getId()) && Objects.equals(getName(), avatar.getName()) && Objects.equals(getOriginalFileName(), avatar.getOriginalFileName()) && Objects.equals(getSize(), avatar.getSize()) && Objects.equals(getContentType(), avatar.getContentType()) && Arrays.equals(getBytes(), avatar.getBytes()) && Objects.equals(getUser(), avatar.getUser());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getId(), getName(), getOriginalFileName(), getSize(), getContentType());
        result = 31 * result + Arrays.hashCode(getBytes());
        return result;
    }
}
