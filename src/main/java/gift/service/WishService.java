package gift.service;

import gift.common.exception.DuplicateDataException;
import gift.common.exception.EntityNotFoundException;
import gift.controller.dto.request.WishInsertRequest;
import gift.controller.dto.request.WishPatchRequest;
import gift.controller.dto.response.WishResponse;
import gift.model.Member;
import gift.model.Product;
import gift.model.Wish;
import gift.repository.MemberRepository;
import gift.repository.ProductRepository;
import gift.repository.WishRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository, MemberRepository memberRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    public void update(WishPatchRequest request, Long memberId) {
        checkProductExist(request.productId(), memberId);
        if (request.productCount() == 0) {
            deleteByProductId(request.productId(), memberId);
            return;
        }
        Wish wish = wishRepository.findByMemberIdAndProductId(memberId, request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Wish not found"));
        wish.updateWish(wish.getMember(), request.productCount(), wish.getProduct());
        wishRepository.save(wish);

    }

    public void save(WishInsertRequest request, int productCount, Long memberId) {
        checkProductExist(request.productId());
        checkDuplicateWish(request.productId(), memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new EntityNotFoundException("Member with id " + memberId + " not found"));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(()-> new EntityNotFoundException("Product with id " + request.productId() + " not found"));
        wishRepository.save(new Wish(member, productCount, product));
    }

    public List<WishResponse> findAllByMemberId(Long memberId) {
        return wishRepository.findAllByMemberIdOrderByCreatedAtAsc(memberId).stream()
                .map(WishResponse::from)
                .toList();
    }

    public void deleteByProductId(Long productId, Long memberId) {
        wishRepository.deleteByProductIdAndMemberId(productId, memberId);
    }

    private void checkProductExist(Long productId, Long memberId) {
        if (!wishRepository.existsByProductIdAndMemberId(productId, memberId)) {
            throw new EntityNotFoundException("Product with id " + productId + " does not exist in wish");
        }
    }

    private void checkProductExist(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product with id " + productId + " does not exist");
        }
    }

    private void checkDuplicateWish(Long productId, Long memberId) {
        if (wishRepository.existsByProductIdAndMemberId(productId, memberId)) {
            throw new DuplicateDataException("Product with id " + productId + " already exists in wish", "Duplicate Wish");
        }
    }
}
